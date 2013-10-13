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
package nl.clockwork.ebms.admin.web.service.cpa;

import java.util.Iterator;

import nl.clockwork.ebms.service.CPAService;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class CPADataProvider implements IDataProvider<String>
{
	private static final long serialVersionUID = 1L;
	private CPAService cpaClient;

	public CPADataProvider(CPAService cpaClient)
	{
		this.cpaClient = cpaClient;
	}
	
	@Override
	public Iterator<? extends String> iterator(long first, long count)
	{
		return cpaClient.getCPAIds().iterator();
	}

	@Override
	public IModel<String> model(final String cpaId)
	{
		return Model.of(cpaId);
	}

	@Override
	public long size()
	{
		return (int)cpaClient.getCPAIds().size();
	}

	@Override
	public void detach()
	{
	}

}