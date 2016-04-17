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
package nl.clockwork.ebms.admin.web.configuration;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import nl.clockwork.ebms.admin.web.configuration.CorePropertiesFormPanel.CorePropertiesFormModel;
import nl.clockwork.ebms.admin.web.configuration.EbMSCorePropertiesPage.EbMSCorePropertiesFormModel;
import nl.clockwork.ebms.admin.web.configuration.HttpPropertiesFormPanel.HttpPropertiesFormModel;
import nl.clockwork.ebms.admin.web.configuration.JdbcPropertiesFormPanel.JdbcPropertiesFormModel;
import nl.clockwork.ebms.admin.web.configuration.ProxyPropertiesFormPanel.ProxyPropertiesFormModel;
import nl.clockwork.ebms.admin.web.configuration.SignaturePropertiesFormPanel.SignaturePropertiesFormModel;
import nl.clockwork.ebms.admin.web.configuration.SslPropertiesFormPanel.SslPropertiesFormModel;

import org.apache.commons.lang.StringUtils;

public class EbMSCorePropertiesWriter
{
	protected Writer writer;

  public EbMSCorePropertiesWriter(Writer writer)
	{
		this.writer = writer;
	}

	public void write(EbMSCorePropertiesFormModel ebMSCoreProperties) throws IOException
	{
		Properties p = new Properties();
		write(p,ebMSCoreProperties.getCoreProperties());
		write(p,ebMSCoreProperties.getHttpProperties());
		write(p,ebMSCoreProperties.getSignatureProperties());
		write(p,ebMSCoreProperties.getJdbcProperties());
		p.store(writer,"EbMS Core properties");
	}

  protected void write(Properties properties, CorePropertiesFormModel coreProperties)
  {
		properties.setProperty("patch.digipoort.enable",Boolean.toString(coreProperties.isDigipoortPatch()));
		properties.setProperty("patch.oracle.enable",Boolean.toString(coreProperties.isOraclePatch()));
		properties.setProperty("patch.cleo.enable",Boolean.toString(coreProperties.isCleoPatch()));
  }

	protected void write(Properties properties, HttpPropertiesFormModel httpProperties)
  {
		properties.setProperty("ebms.host",httpProperties.getHost());
		properties.setProperty("ebms.port",httpProperties.getPort() == null ? "" : httpProperties.getPort().toString());
		properties.setProperty("ebms.path",httpProperties.getPath());
		properties.setProperty("ebms.ssl",Boolean.toString(httpProperties.getSsl()));
		properties.setProperty("http.chunkedStreamingMode",Boolean.toString(httpProperties.isChunkedStreamingMode()));
		properties.setProperty("http.base64Writer",Boolean.toString(httpProperties.isBase64Writer()));
		if (httpProperties.getSsl())
			write(properties,httpProperties.getSslProperties());
		if (httpProperties.getProxy())
			write(properties,httpProperties.getProxyProperties());
  }

	protected void write(Properties properties, SslPropertiesFormModel sslProperties)
  {
		if (sslProperties.isOverrideEnabledProtocols())
			properties.setProperty("https.enabledProtocols",StringUtils.join(sslProperties.getEnabledProtocols(),','));
		properties.setProperty("https.allowedCipherSuites",StringUtils.join(sslProperties.getEnabledCipherSuites(),','));
		properties.setProperty("https.requireClientAuthentication",Boolean.toString(sslProperties.getRequireClientAuthentication()));
		properties.setProperty("https.verifyHostnames",Boolean.toString(sslProperties.getVerifyHostnames()));
 		properties.setProperty("keystore.path",StringUtils.defaultString(sslProperties.getKeystoreProperties().getUri()));
 		properties.setProperty("keystore.password",StringUtils.defaultString(sslProperties.getKeystoreProperties().getPassword()));
 		properties.setProperty("truststore.path",StringUtils.defaultString(sslProperties.getTruststoreProperties().getUri()));
 		properties.setProperty("truststore.password",StringUtils.defaultString(sslProperties.getTruststoreProperties().getPassword()));
  }

	protected void write(Properties properties, ProxyPropertiesFormModel proxyProperties)
  {
		properties.setProperty("http.proxy.host",StringUtils.defaultString(proxyProperties.getHost()));
		properties.setProperty("http.proxy.port",proxyProperties.getPort() == null ? "80" : proxyProperties.getPort().toString());
		properties.setProperty("http.proxy.nonProxyHosts",StringUtils.defaultString(proxyProperties.getNonProxyHosts()));
 		properties.setProperty("http.proxy.username",StringUtils.defaultString(proxyProperties.getUsername()));
 		properties.setProperty("http.proxy.password",StringUtils.defaultString(proxyProperties.getPassword()));
  }

	protected void write(Properties properties, SignaturePropertiesFormModel signatureProperties)
  {
  	if (signatureProperties.getSigning())
  	{
  		properties.setProperty("signature.keystore.path",StringUtils.defaultString(signatureProperties.getKeystoreProperties().getUri()));
  		properties.setProperty("signature.keystore.password",StringUtils.defaultString(signatureProperties.getKeystoreProperties().getPassword()));
  	}
  }

	protected void write(Properties properties, JdbcPropertiesFormModel jdbcProperties)
  {
		properties.setProperty("ebms.jdbc.driverClassName",jdbcProperties.getDriver().getDriverClassName());
		properties.setProperty("ebms.jdbc.url",jdbcProperties.getUrl());
		properties.setProperty("ebms.jdbc.username",jdbcProperties.getUsername());
		properties.setProperty("ebms.jdbc.password",StringUtils.defaultString(jdbcProperties.getPassword()));
		properties.setProperty("ebms.pool.preferredTestQuery",jdbcProperties.getDriver().getPreferredTestQuery());
  }
  

}
