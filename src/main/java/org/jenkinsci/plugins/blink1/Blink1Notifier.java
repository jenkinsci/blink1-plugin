package org.jenkinsci.plugins.blink1;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BallColor;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class Blink1Notifier extends Notifier
{
	@DataBoundConstructor
	public Blink1Notifier()
	{
	}

	static class Color
	{
		byte r, g, b;

		public Color(byte r, byte g, byte b)
		{
			this.r = r;
			this.g = g;
			this.b = b;
		}

		public String getHexString()
		{
			return Integer.toHexString(r & 0xFF)
					+ Integer.toHexString(g & 0xFF)
					+ Integer.toHexString(b & 0xFF);
		}

		public String getIntString()
		{
			return r + "," + g + "," + b;
		}
	}

	private static final Color COLOR_BLUE = new Color((byte) 0, (byte) 0,
			(byte) 255);
	private static final Color COLOR_YELLOW = new Color((byte) 255, (byte) 200,
			(byte) 0);
	private static final Color COLOR_RED = new Color((byte) 255, (byte) 0,
			(byte) 0);
	private static final Color COLOR_WHITE = new Color((byte) 255, (byte) 255,
			(byte) 255);

	private static final String DEFAULT_BLINK_INTERFACE = "webApi";
	private static final String DEFAULT_URL_BASE = "http://localhost:8934";
	private static final String DEFAULT_COMMAND_PATH = "/usr/bin/blink1-tool";

	private static final double DELAY = 0.5;

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher,
			BuildListener listener)
	{
		BallColor ballColor = build.getResult().color;
		Color color = COLOR_WHITE;
		if (BallColor.BLUE == ballColor)
			color = COLOR_BLUE;
		else if (BallColor.YELLOW == ballColor)
			color = COLOR_YELLOW;
		else if (BallColor.RED == ballColor)
			color = COLOR_RED;
		if (getDescriptor().isBlinkInterfaceCommandline())
			blinkWithCommandline(listener, color);
		else
			blinkWithLocalWebAPI(listener, color);
		return true;
	}

	private void blinkWithCommandline(BuildListener listener, Color color)
	{
		try
		{
			Process process = Runtime.getRuntime().exec(
					getDescriptor().getCommandPath() + " --rgb "
							+ color.getIntString());
			process.waitFor();
			BufferedReader buf = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line;
			while ((line = buf.readLine()) != null)
			{
				listener.getLogger().println(line);
			}
		} catch (Exception ex)
		{
			ex.printStackTrace(listener.getLogger());
		}
	}

	private void blinkWithLocalWebAPI(BuildListener listener, Color color)
	{
		String urlStr = getDescriptor().getUrlBase()
				+ "/blink1/fadeToRGB?rgb=%23" + color.getHexString() + "&time="
				+ DELAY;
		URL url;
		try
		{
			URLConnection conn;
			url = new URL(urlStr);
			conn = url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null)
			{
				// listener.getLogger().println(inputLine);
			}
			in.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public BuildStepMonitor getRequiredMonitorService()
	{
		return BuildStepMonitor.NONE;
	}

	@Override
	public DescriptorImpl getDescriptor()
	{
		return (DescriptorImpl) super.getDescriptor();
	}

	private static final String FORM_KEY_BLINK_INTERFACE = "blinkInterface";
	private static final String FORM_KEY_URL_BASE = "urlBase";
	private static final String FORM_KEY_COMMAND_PATH = "commandPath";
	private static final String FORM_VALUE_BLINK_INTERFACE_WEB_API = "webApi";
	private static final String FORM_VALUE_BLINK_INTERFACE_COMMANDLINE = "commandline";

	@Extension
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Publisher>
	{

		private String urlBase = DEFAULT_URL_BASE;
		private String commandPath = DEFAULT_COMMAND_PATH;
		private String blinkInterface = DEFAULT_BLINK_INTERFACE;

		public DescriptorImpl()
		{
			load();
		}

		public String defaultUrlBase()
		{
			return DEFAULT_URL_BASE;
		}

		public String defaultCommandPath()
		{
			return DEFAULT_COMMAND_PATH;
		}

		public String defaultBlinkInterface()
		{
			return DEFAULT_BLINK_INTERFACE;
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass)
		{
			return true;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject form)
				throws FormException
		{
			if (!form.containsKey(FORM_KEY_URL_BASE)
					&& !form.containsKey(FORM_KEY_COMMAND_PATH))
				return false;
			this.urlBase = form.getString(FORM_KEY_URL_BASE);
			this.commandPath = form.getString(FORM_KEY_COMMAND_PATH);
			this.blinkInterface = form.getString(FORM_KEY_BLINK_INTERFACE);
			save();
			return super.configure(req, form);
		}

		public FormValidation doCheckUrlBase(@QueryParameter String value)
		{
			if (isValidUrl(value))
				return FormValidation.ok();
			else
				return FormValidation
						.error("URL should start with http:// or https://.");
		}

		private boolean isValidUrl(String value)
		{
			return value.startsWith("http://") || value.startsWith("https://");
		}

		public String getDisplayName()
		{
			return "Blink1Notifier";
		}

		public boolean isBlinkInterfaceWebApi()
		{
			return FORM_VALUE_BLINK_INTERFACE_WEB_API.equals(blinkInterface);
		}

		public boolean isBlinkInterfaceCommandline()
		{
			return FORM_VALUE_BLINK_INTERFACE_COMMANDLINE
					.equals(blinkInterface);
		}

		public String getUrlBase()
		{
			return this.urlBase;
		}

		public String getCommandPath()
		{
			return this.commandPath;
		}

		public String getBlinkInterface()
		{
			return this.blinkInterface;
		}
	}
}