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

public class Blink1Notifier extends Notifier {
	@DataBoundConstructor
	public Blink1Notifier() {
	}

	private static final String COLOR_BLUE = "0000FF";
	private static final String COLOR_YELLOW = "FFFF00";
	private static final String COLOR_RED = "FF0000";
	
	private static final String DEFAULT_BLINK_INTERFACE = "webApi";
	private static final String DEFAULT_URL_BASE = "http://localhost:8934";
	private static final String DEFAULT_COMMAND_PATH = "/usr/bin/blink1-tool";
	
	private static final double DELAY = 0.5;

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
		listener.getLogger().println("Blink1Notifier.perform");
		listener.getLogger().println("descriptor.interface=" + getDescriptor().getBlinkInterface());
		BallColor color = build.getResult().color;
		String colorCode = "FFFFFF";
		if (BallColor.BLUE==color)
			colorCode = COLOR_BLUE;
		else if (BallColor.YELLOW==color)
			colorCode = COLOR_YELLOW;
		else if (BallColor.RED==color)
			colorCode = COLOR_RED;
		blink(listener, colorCode);
		return true;
	}

	private void blink(BuildListener listener, String colorCode) {
		String urlStr = getDescriptor().getUrlBase() + "/blink1/fadeToRGB?rgb=%23" + colorCode + "&time=" + DELAY;
		
		URL url;
		try {
			URLConnection conn;
			url = new URL(urlStr);
			conn = url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				// listener.getLogger().println(inputLine);
			}
			in.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl)super.getDescriptor();
	}
	
	private static final String FORM_KEY_BLINK_INTERFACE = "blinkInterface";
	private static final String FORM_KEY_URL_BASE = "urlBase";
	private static final String FORM_KEY_COMMAND_PATH = "commandPath";
	private static final String FORM_VALUE_BLINK_INTERFACE_WEB_API = "webApi";
	private static final String FORM_VALUE_BLINK_INTERFACE_COMMANDLINE = "commandline";
	
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		private String urlBase = DEFAULT_URL_BASE;
		private String commandPath = DEFAULT_COMMAND_PATH;
		private String blinkInterface = DEFAULT_BLINK_INTERFACE;
		
		public DescriptorImpl() {
			load();
		}
		
		public String defaultUrlBase() {
			return DEFAULT_URL_BASE;
		}
		public String defaultCommandPath() {
			return DEFAULT_COMMAND_PATH;
		}
		public String defaultBlinkInterface  () {
			return DEFAULT_BLINK_INTERFACE;
		}
		
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}
		
		@Override
		public boolean configure(StaplerRequest req, JSONObject form) throws FormException {
			if (!form.containsKey(FORM_KEY_URL_BASE) && !form.containsKey(FORM_KEY_COMMAND_PATH))
				return false;
			this.urlBase = form.getString(FORM_KEY_URL_BASE);
			this.commandPath = form.getString(FORM_KEY_COMMAND_PATH);
			this.blinkInterface = form.getString(FORM_KEY_BLINK_INTERFACE);
			save();
			return super.configure(req, form);
		}
		public FormValidation doCheckUrlBase(@QueryParameter String value) {
			if(isValidUrl(value)) 
				  return FormValidation.ok();
			else 
				  return FormValidation.error("URL should start with http:// or https://.");
			}
		private boolean isValidUrl(String value)
		{
			return value.startsWith("http://") || value.startsWith("https://");
		}

		public String getDisplayName() {
			return "Blink1Notifier";
		}

		public boolean isBlinkInterfaceWebApi () {
			return FORM_VALUE_BLINK_INTERFACE_WEB_API.equals(blinkInterface);
		}
		public boolean isBlinkInterfaceCommandline () {
			return FORM_VALUE_BLINK_INTERFACE_COMMANDLINE.equals(blinkInterface);
		}
		
		public String getUrlBase () {
			return this.urlBase;
		}
		public String getCommandPath () {
			return this.commandPath;
		}
		public String getBlinkInterface () {
			return this.blinkInterface;
		}
	}
}