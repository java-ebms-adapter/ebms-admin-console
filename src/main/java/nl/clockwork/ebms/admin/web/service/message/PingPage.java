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
package nl.clockwork.ebms.admin.web.service.message;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import nl.clockwork.ebms.admin.CPAUtils;
import nl.clockwork.ebms.admin.Utils;
import nl.clockwork.ebms.admin.web.BasePage;
import nl.clockwork.ebms.admin.web.BootstrapFeedbackPanel;
import nl.clockwork.ebms.admin.web.BootstrapFormComponentFeedbackBorder;
import nl.clockwork.ebms.admin.web.ResetButton;
import nl.clockwork.ebms.common.XMLMessageBuilder;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.service.CPAService;
import nl.clockwork.ebms.service.EbMSMessageService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.io.IClusterable;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

public class PingPage extends BasePage
{
	private static final long serialVersionUID = 1L;
	protected transient Log logger = LogFactory.getLog(getClass());
	@SpringBean(name="cpaService")
	private CPAService cpaService;
	@SpringBean(name="ebMSMessageService")
	private EbMSMessageService ebMSMessageService;
	@SpringBean(name="cleoPatch")
	private Boolean cleoPatch;

	public PingPage()
	{
		add(new BootstrapFeedbackPanel("feedback").setOutputMarkupId(true));
		add(new PingForm("form"));
	}
	
	@Override
	public String getPageTitle()
	{
		return getLocalizer().getString("ping",this);
	}

	public class PingForm extends Form<PingFormModel>
	{
		private static final long serialVersionUID = 1L;

		public PingForm(String id)
		{
			super(id,new CompoundPropertyModel<PingFormModel>(new PingFormModel()));
			add(new BootstrapFormComponentFeedbackBorder("cpaIdFeedback",createCPAIdChoice("cpaId")));
			add(new BootstrapFormComponentFeedbackBorder("fromPartyIdFeedback",createFromPartyIdChoice("fromPartyId")));
			add(new BootstrapFormComponentFeedbackBorder("fromRoleFeedback",createFromRoleChoice("fromRole")).setVisible(cleoPatch));
			add(new BootstrapFormComponentFeedbackBorder("toPartyIdFeedback",createToPartyIdChoice("toPartyId")));
			add(new BootstrapFormComponentFeedbackBorder("toRoleFeedback",createToRoleChoice("toRole")).setVisible(cleoPatch));
			Button ping = createPingButton("ping");
			setDefaultButton(ping);
			add(ping);
			add(new ResetButton("reset",new ResourceModel("cmd.reset"),PingPage.class));
		}

		private DropDownChoice<String> createCPAIdChoice(String id)
		{
			DropDownChoice<String> result = new DropDownChoice<String>(id,Model.ofList(Utils.toList(cpaService.getCPAIds())));
			result.setLabel(new ResourceModel("lbl.cpaId"));
			result.setRequired(true);
			result.add(new AjaxFormComponentUpdatingBehavior("onchange")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onUpdate(AjaxRequestTarget target)
				{
					try
					{
						PingFormModel model = PingForm.this.getModelObject();
						CollaborationProtocolAgreement cpa = XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpaService.getCPA(model.getCpaId()));
						model.resetFromPartyIds(CPAUtils.getPartyIds(cpa));
						model.resetFromRoles(CPAUtils.getRoleNames(cpa));
						model.resetToPartyIds();
						model.resetToRoles();
						target.add(getPage().get("feedback"));
						target.add(getPage().get("form"));
					}
					catch (JAXBException e)
					{
						logger.error("",e);
						error(e.getMessage());
					}
				}
			});
			return result;
		}

		private DropDownChoice<String> createFromPartyIdChoice(String id)
		{
			DropDownChoice<String> result = new DropDownChoice<String>(id,new PropertyModel<List<String>>(this.getModelObject(),"fromPartyIds"));
			result.setLabel(new ResourceModel("lbl.fromPartyId"));
			result.setRequired(true).setOutputMarkupId(true);
			result.add(new AjaxFormComponentUpdatingBehavior("onchange")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onUpdate(AjaxRequestTarget target)
				{
					try
					{
						PingFormModel model = PingForm.this.getModelObject();
						CollaborationProtocolAgreement cpa = XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpaService.getCPA(model.getCpaId()));
						model.resetFromRoles(CPAUtils.getRoleNames(cpa,model.getFromPartyId()));
						model.resetToPartyIds(CPAUtils.getOtherPartyIds(cpa,model.getFromPartyId()));
						if (model.getFromRole() != null)
							model.resetToRoles(CPAUtils.getRoleNames(cpa,model.getToPartyId()));
						target.add(getPage().get("feedback"));
						target.add(getPage().get("form"));
					}
					catch (JAXBException e)
					{
						logger.error("",e);
						error(e.getMessage());
					}
				}
			});
			return result;
		}

		private DropDownChoice<String> createFromRoleChoice(String id)
		{
			DropDownChoice<String> result = new DropDownChoice<String>(id,new PropertyModel<List<String>>(this.getModelObject(),"fromRoles"));
			result.setLabel(new ResourceModel("lbl.fromRole"));
			result.setRequired(false).setOutputMarkupId(true);
			result.add(new AjaxFormComponentUpdatingBehavior("onchange")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onUpdate(AjaxRequestTarget target)
				{
					try
					{
						PingFormModel model = PingForm.this.getModelObject();
						CollaborationProtocolAgreement cpa = XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpaService.getCPA(model.getCpaId()));
						model.resetFromPartyIds(CPAUtils.getPartyIdsByRoleName(cpa,model.getFromRole()));
						model.resetToPartyIds(CPAUtils.getOtherPartyIds(cpa,model.getFromPartyId()));
						model.resetToRoles(CPAUtils.getRoleNames(cpa,model.getToPartyId()));
						target.add(getPage().get("feedback"));
						target.add(getPage().get("form"));
					}
					catch (JAXBException e)
					{
						logger.error("",e);
						error(e.getMessage());
					}
				}
			});
			return result;
		}

		private DropDownChoice<String> createToPartyIdChoice(String id)
		{
			DropDownChoice<String> result = new DropDownChoice<String>(id,new PropertyModel<List<String>>(this.getModelObject(),"toPartyIds"));
			result.setLabel(new ResourceModel("lbl.toPartyId"));
			result.setRequired(true).setOutputMarkupId(true);
			result.add(new AjaxFormComponentUpdatingBehavior("onchange")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onUpdate(AjaxRequestTarget target)
				{
					try
					{
						PingFormModel model = PingForm.this.getModelObject();
						CollaborationProtocolAgreement cpa = XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpaService.getCPA(model.getCpaId()));
						model.resetToRoles(CPAUtils.getRoleNames(cpa,model.getToPartyId()));
						target.add(getPage().get("feedback"));
						target.add(getPage().get("form"));
					}
					catch (JAXBException e)
					{
						logger.error("",e);
						error(e.getMessage());
					}
				}
			});
			return result;
		}

		private DropDownChoice<String> createToRoleChoice(String id)
		{
			DropDownChoice<String> result = new DropDownChoice<String>(id,new PropertyModel<List<String>>(this.getModelObject(),"toRoles"));
			result.setLabel(new ResourceModel("lbl.toRole"));
			result.setRequired(false).setOutputMarkupId(true);
			result.add(new AjaxFormComponentUpdatingBehavior("onchange")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onUpdate(AjaxRequestTarget target)
				{
					try
					{
						PingFormModel model = PingForm.this.getModelObject();
						CollaborationProtocolAgreement cpa = XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpaService.getCPA(model.getCpaId()));
						if (model.getToPartyId() == null)
							model.resetToPartyIds(CPAUtils.getPartyIdsByRoleName(cpa,model.getToRole()));
						target.add(getPage().get("feedback"));
						target.add(getPage().get("form"));
					}
					catch (JAXBException e)
					{
						logger.error("",e);
						error(e.getMessage());
					}
				}
			});
			return result;
		}

		private Button createPingButton(String id)
		{
			Button result = new Button(id,new ResourceModel("cmd.ping"))
			{
				private static final long serialVersionUID = 1L;
	
				@Override
				public void onSubmit()
				{
					try
					{
						PingFormModel model = PingForm.this.getModelObject();
						ebMSMessageService.ping(model.getCpaId(),new Party(model.getFromPartyId(),model.getFromRole()),new Party(model.getToPartyId(),model.getToRole()));
						info(PingPage.this.getString("ping.ok"));
					}
					catch (Exception e)
					{
						logger.error("",e);
						error(e.getMessage());
					}
				}
			};
			return result;
		}

	}

	public class PingFormModel implements IClusterable
	{
		private static final long serialVersionUID = 1L;
		private String cpaId;
		private List<String> fromPartyIds = new ArrayList<String>();
		private String fromPartyId;
		private List<String> fromRoles = new ArrayList<String>();
		private String fromRole;
		private List<String> toPartyIds = new ArrayList<String>();
		private String toPartyId;
		private List<String> toRoles = new ArrayList<String>();
		private String toRole;
		
		public String getCpaId()
		{
			return cpaId;
		}
		public void setCpaId(String cpaId)
		{
			this.cpaId = cpaId;
		}
		public List<String> getFromPartyIds()
		{
			return fromPartyIds;
		}
		public void setFromPartyId(String fromPartyId)
		{
			this.fromPartyId = fromPartyId;
		}
		public String getFromPartyId()
		{
			return fromPartyId;
		}
		public void resetFromPartyIds()
		{
			getFromPartyIds().clear();
			setFromPartyId(null);
		}
		public void resetFromPartyIds(ArrayList<String> partyIds)
		{
			resetFromPartyIds();
			getFromPartyIds().addAll(partyIds);
			setFromPartyId(getFromPartyIds().size() == 1 ? getFromPartyIds().get(0) : null);
		}
		public List<String> getFromRoles()
		{
			return fromRoles;
		}
		public String getFromRole()
		{
			return fromRole;
		}
		public void setFromRole(String fromRole)
		{
			this.fromRole = fromRole;
		}
		public void resetFromRoles()
		{
			getFromRoles().clear();
			setFromRole(null);
		}
		public void resetFromRoles(List<String> roleNames)
		{
			resetFromRoles();
			getFromRoles().addAll(roleNames);
		}
		public List<String> getToPartyIds()
		{
			return toPartyIds;
		}
		public String getToPartyId()
		{
			return toPartyId;
		}
		public void setToPartyId(String toPartyId)
		{
			this.toPartyId = toPartyId;
		}
		public void resetToPartyIds()
		{
			getToPartyIds().clear();
			setToPartyId(null);
		}
		public void resetToPartyIds(List<String> partyIds)
		{
			resetToPartyIds();
			getToPartyIds().addAll(partyIds);
			setToPartyId(getToPartyIds().size() == 1 ? getToPartyIds().get(0) : null);
		}
		public List<String> getToRoles()
		{
			return toRoles;
		}
		public String getToRole()
		{
			return toRole;
		}
		public void setToRole(String toRole)
		{
			this.toRole = toRole;
		}
		public void resetToRoles()
		{
			getToRoles().clear();
			setToRole(null);
		}
		public void resetToRoles(ArrayList<String> roleNames)
		{
			resetToRoles();
			getToRoles().addAll(roleNames);
			setToRole(getFromRole() != null && getToRoles().size() == 1 ? getToRoles().get(0) : null);
		}
	}		

}
