/**
 * Copyright 2013 Clockwork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.clockwork.ebms.admin.dao;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.Constants.EbMSEventType;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.admin.model.CPA;
import nl.clockwork.ebms.admin.model.EbMSAttachment;
import nl.clockwork.ebms.admin.model.EbMSEvent;
import nl.clockwork.ebms.admin.model.EbMSMessage;
import nl.clockwork.ebms.admin.web.Utils;
import nl.clockwork.ebms.admin.web.message.EbMSMessageFilter;
import nl.clockwork.ebms.dao.DAOException;

import org.apache.commons.csv.CSVPrinter;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.transaction.support.TransactionTemplate;

public abstract class AbstractEbMSDAO implements EbMSDAO
{
	private static class CPARowMapper implements ParameterizedRowMapper<CPA>
	{
		public static String getBaseQuery()
		{
			return "select cpa_id, cpa from cpa";
		}

		@Override
		public CPA mapRow(ResultSet rs, int rowNum) throws SQLException
		{
			return new CPA(rs.getString("cpa_id"),rs.getString("cpa"));
		}
	}
	
	private static class EbMSMessageRowMapper implements ParameterizedRowMapper<EbMSMessage>
	{
		private boolean detail;

		public EbMSMessageRowMapper()
		{
			this(false);
		}
		
		public EbMSMessageRowMapper(boolean detail)
		{
			this.detail = detail;
		}

		public String getBaseQuery()
		{
			return "select" +
				" time_stamp," +
				" cpa_id," +
				" conversation_id," +
				" sequence_nr," +
				" message_id," +
				" message_nr," +
				" ref_to_message_id," +
				" time_to_live," +
				" from_role," +
				" to_role," +
				" service," +
				" action," +
				(detail ? " content," : "") +
				" status," +
				" status_time" +
				" from ebms_message";
		}
		
		@Override
		public EbMSMessage mapRow(ResultSet rs, int rowNum) throws SQLException
		{
			EbMSMessage result = new EbMSMessage();
			result.setTimestamp(rs.getTimestamp("time_stamp"));
			result.setCpaId(rs.getString("cpa_id"));
			result.setConversationId(rs.getString("conversation_id"));
			result.setSequenceNr(rs.getLong("sequence_nr"));
			result.setMessageId(rs.getString("message_id"));
			result.setMessageNr(rs.getInt("message_nr"));
			result.setRefToMessageId(rs.getString("ref_to_message_id"));
			result.setTimeToLive(rs.getTimestamp("time_to_live"));
			result.setFromRole(rs.getString("from_role"));
			result.setToRole(rs.getString("to_role"));
			result.setService(rs.getString("service"));
			result.setAction(rs.getString("action"));
			if (detail)
				result.setContent(rs.getString("content"));
			result.setStatus(rs.getObject("status") == null ? null : EbMSMessageStatus.get(rs.getInt("status")));
			result.setStatusTime(rs.getTimestamp("status_time"));
			return result;
		}
	}
	
	protected TransactionTemplate transactionTemplate;
	protected JdbcTemplate jdbcTemplate;
	public abstract String getTimestampFunction();

	public AbstractEbMSDAO(TransactionTemplate transactionTemplate, JdbcTemplate jdbcTemplate)
	{
		this.transactionTemplate = transactionTemplate;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public CPA getCPA(String cpaId)
	{
		try
		{
			return jdbcTemplate.queryForObject(
				CPARowMapper.getBaseQuery() +
				" where cpa_id = ?",
				new CPARowMapper(),
				cpaId
			);
		}
		catch (EmptyResultDataAccessException e)
		{
			return null;
		}
	}

	@Override
	public int getCPACount()
	{
		return jdbcTemplate.queryForInt("select count(cpa_id) from cpa");
	}
	
	@Override
	public List<String> getCPAIds()
	{
		return jdbcTemplate.queryForList(
			"select cpa_id" +
			" from cpa" +
			" order by cpa_id",
			String.class
		);
	}
	
	@Override
	public List<CPA> getCPAs(long first, long count)
	{
		return jdbcTemplate.query(
			CPARowMapper.getBaseQuery() +
			" order by cpa_id" +
			" limit ? offset ?",
			new CPARowMapper(),
			first + count,
			first
		);
	}

	@Override
	public EbMSMessage getMessage(String messageId)
	{
		return getMessage(messageId,0);
	}

	@Override
	public EbMSMessage getMessage(String messageId, int messageNr)
	{
		EbMSMessage result = jdbcTemplate.queryForObject(
			new EbMSMessageRowMapper(true).getBaseQuery() + 
			" where message_id = ?" +
			" and message_nr = ?",
			new EbMSMessageRowMapper(true),
			messageId,
			messageNr
		);
		result.setAttachments(getAttachments(messageId,messageNr));
		for (EbMSAttachment attachment : result.getAttachments())
			attachment.setMessage(result);
		result.setEvents(getEvents(messageId));
		for (EbMSEvent event : result.getEvents())
			event.setMessage(result);
		
		return result;
	}

	@Override
	public int getMessageCount(EbMSMessageFilter filter)
	{
		List<Object> parameters = new ArrayList<Object>();
		return jdbcTemplate.queryForInt(
			"select count(message_id)" +
			" from ebms_message" +
			" where 1 = 1" +
			getMessageFilter(filter,parameters),
			parameters.toArray(new Object[0])
		);
	}
	
	@Override
	public List<EbMSMessage> getMessages(EbMSMessageFilter filter, long first, long count)
	{
		List<Object> parameters = new ArrayList<Object>();
		return jdbcTemplate.query(
			new EbMSMessageRowMapper().getBaseQuery() +
			" where 1 = 1" +
			getMessageFilter(filter,parameters) +
			" order by time_stamp desc" +
			" limit " + (first + count) + " offset " + first,
			parameters.toArray(new Object[0]),
			new EbMSMessageRowMapper()
		);
	}
	
	@Override
	public EbMSAttachment getAttachment(String messageId, int messageNr, String contentId)
	{
		return jdbcTemplate.queryForObject(
			"select name, content_id, content_type, content" + 
			" from ebms_attachment" + 
			" where message_id = ?" +
			" and message_nr = ?" +
			" and content_id = ?",
			new ParameterizedRowMapper<EbMSAttachment>()
			{
				@Override
				public EbMSAttachment mapRow(ResultSet rs, int rowNum) throws SQLException
				{
					return new EbMSAttachment(rs.getString("name"),rs.getString("content_id"),rs.getString("content_type"),rs.getBytes("content"));
				}
			},
			messageId,
			messageNr,
			contentId
		);
	}

	private List<EbMSAttachment> getAttachments(String messageId, int messageNr)
	{
		return jdbcTemplate.query(
			"select name, content_id, content_type" + 
			" from ebms_attachment" + 
			" where message_id = ?" +
			" and message_nr = ?",
			new ParameterizedRowMapper<EbMSAttachment>()
			{
				@Override
				public EbMSAttachment mapRow(ResultSet rs, int rowNum) throws SQLException
				{
					return new EbMSAttachment(rs.getString("name"),rs.getString("content_id"),rs.getString("content_type"),null);
				}
			},
			messageId,
			messageNr
		);
	}

	private List<EbMSEvent> getEvents(String messageId)
	{
		return jdbcTemplate.query(
			"select message_id, time, type, status, status_time, uri, error_message" + 
			" from ebms_event" + 
			" where message_id = ?",
			new ParameterizedRowMapper<EbMSEvent>()
			{
				@Override
				public EbMSEvent mapRow(ResultSet rs, int rowNum) throws SQLException
				{
					return new EbMSEvent(rs.getTimestamp("time"),EbMSEventType.get(rs.getInt("type")),EbMSEventStatus.get(rs.getInt("status")),rs.getTimestamp("status_time"),rs.getString("uri"),rs.getString("error_message"));
				}
			},
			messageId
		);
	}
	
	@Override
	public void printMessagesToCSV(final CSVPrinter printer, EbMSMessageFilter filter)
	{
		List<Object> parameters = new ArrayList<Object>();
		jdbcTemplate.query(
			new EbMSMessageRowMapper().getBaseQuery() +
			" where 1 = 1" +
			getMessageFilter(filter,parameters) +
			" order by time_stamp desc",
			parameters.toArray(new Object[0]),
			new ParameterizedRowMapper<Object>()
			{
				@Override
				public Object mapRow(ResultSet rs, int rowNum) throws SQLException
				{
					try
					{
						printer.print(rs.getString("message_id"));
						printer.print(rs.getInt("message_nr"));
						printer.print(rs.getLong("sequence_nr"));
						printer.print(rs.getString("ref_to_message_id"));
						printer.print(rs.getString("conversation_id"));
						printer.print(rs.getTimestamp("time_stamp"));
						printer.print(rs.getTimestamp("time_to_live"));
						printer.print(rs.getString("cpa_id"));
						printer.print(rs.getString("from_role"));
						printer.print(rs.getString("to_role"));
						printer.print(rs.getString("service"));
						printer.print(rs.getString("action"));
						printer.print(rs.getObject("status") == null ? null : EbMSMessageStatus.get(rs.getInt("status")));
						printer.print(rs.getTimestamp("status_time"));
						printer.println();
						return null;
					}
					catch (IOException e)
					{
						throw new SQLException(e);
					}
				}
				
			}
		);
	}

	@Override
	public void writeMessageToZip(String messageId, int messageNr, final ZipOutputStream zip)
	{
		try
		{
			jdbcTemplate.queryForObject(
				"select content" + 
				" from ebms_message" + 
				" where message_id = ?" +
				" and message_nr = ?",
				new ParameterizedRowMapper<Object>()
				{
					@Override
					public Object mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						try
						{
							ZipEntry entry = new ZipEntry("message.xml");
							zip.putNextEntry(entry);
							zip.write(rs.getString("content").getBytes());
							zip.closeEntry();
							return null;
						}
						catch (IOException e)
						{
							throw new SQLException(e);
						}
					}
				},
				messageId,
				messageNr
			);

			jdbcTemplate.query(
				"select name, content_id, content_type, content" + 
				" from ebms_attachment" + 
				" where message_id = ?" +
				" and message_nr = ?",
				new ParameterizedRowMapper<Object>()
				{
					@Override
					public Object mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						try
						{
							ZipEntry entry = new ZipEntry(rs.getString("name") == null ? rs.getString("content_id") + Utils.getFileExtension(rs.getString("content_type")) : rs.getString("name"));
							entry.setComment("Content-Type: " + rs.getString("content_type"));
							zip.putNextEntry(entry);
							zip.write(rs.getBytes("content"));
							zip.closeEntry();
							return null;
						}
						catch (IOException e)
						{
							throw new SQLException(e);
						}
					}
				},
				messageId,
				messageNr
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	protected String getMessageFilter(EbMSMessageFilter messageFilter, List<Object> parameters)
	{
		StringBuffer result = new StringBuffer();
		if (messageFilter != null)
		{
			if (messageFilter.getCpaId() != null)
			{
				parameters.add(messageFilter.getCpaId());
				result.append(" and cpa_id = ?");
			}
			if (messageFilter.getFromRole() != null)
			{
				parameters.add(messageFilter.getFromRole());
				result.append(" and from_role = ?");
			}
			if (messageFilter.getToRole() != null)
			{
				parameters.add(messageFilter.getToRole());
				result.append(" and to_role = ?");
			}
			if (messageFilter.getService() != null)
			{
				parameters.add(messageFilter.getService());
				result.append(" and service = ?");
			}
			if (messageFilter.getAction() != null)
			{
				parameters.add(messageFilter.getAction());
				result.append(" and action = ?");
			}
			if (messageFilter.getConversationId() != null)
			{
				parameters.add(messageFilter.getConversationId());
				result.append(" and conversation_id = ?");
			}
			if (messageFilter.getMessageId() != null)
			{
				parameters.add(messageFilter.getMessageId());
				result.append(" and message_id = ?");
			}
			if (messageFilter.getRefToMessageId() != null)
			{
				parameters.add(messageFilter.getRefToMessageId());
				result.append(" and ref_to_message_id = ?");
			}
			if (messageFilter.getSequenceNr() != null)
			{
				parameters.add(messageFilter.getSequenceNr());
				result.append(" and sequence_nr = ?");
			}
			if (messageFilter.getMshMessage() != null)
			{
				parameters.add(Constants.EBMS_SERVICE_URI);
				if (messageFilter.getMshMessage())
					result.append(" and service = ?");
				else
					result.append(" and service <> ?");
			}
			if (messageFilter.getFrom() != null)
			{
				parameters.add(messageFilter.getFrom());
				result.append(" and timestamp >= ?");
			}
			if (messageFilter.getTo() != null)
			{
				parameters.add(messageFilter.getTo());
				result.append(" and timestamp < ?");
			}
		}
		return result.toString();
	}
	
}