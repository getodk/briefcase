package org.opendatakit.briefcase.model;

/**
 * This class models a proxy connection.
 * 
 * @author rclakmal@gmail.com
 *
 */
public class ProxyConnection {
	private String host;
	private Integer port;
	private ProxyType proxyType;

	public enum ProxyType {
		NO_PROXY, HTTP, HTTPS
	}

	public ProxyConnection(String host, Integer port, ProxyType proxyType) {
		super();
		this.host = host;
		this.port = port;
		this.proxyType = proxyType;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public ProxyType getProxyType() {
		return proxyType;
	}

	public void setProxyType(ProxyType proxyType) {
		this.proxyType = proxyType;
	}

}
