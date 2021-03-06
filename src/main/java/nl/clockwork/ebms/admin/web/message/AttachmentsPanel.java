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
package nl.clockwork.ebms.admin.web.message;

import java.util.List;

import nl.clockwork.ebms.admin.dao.EbMSDAO;
import nl.clockwork.ebms.admin.model.EbMSAttachment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class AttachmentsPanel extends Panel
{
	private class EbMSAttachmentPropertyListView extends PropertyListView<EbMSAttachment>
	{
		private static final long serialVersionUID = 1L;

		public EbMSAttachmentPropertyListView(String id, List<? extends EbMSAttachment> list)
		{
			super(id,list);
		}

		@Override
		protected void populateItem(ListItem<EbMSAttachment> item)
		{
			item.add(new Label("name"));
			DownloadEbMSAttachmentLink link = new DownloadEbMSAttachmentLink("downloadAttachment",ebMSDAO,item.getModelObject());
			link.add(new Label("contentId"));
			item.add(link);
			item.add(new Label("contentType"));
		}
	}

	private static final long serialVersionUID = 1L;
	protected transient Log logger = LogFactory.getLog(this.getClass());
	@SpringBean(name="ebMSAdminDAO")
	private EbMSDAO ebMSDAO;

	public AttachmentsPanel(String id, List<EbMSAttachment> attachments)
	{
		super(id);
		add(new EbMSAttachmentPropertyListView("attachments",attachments));
	}

}