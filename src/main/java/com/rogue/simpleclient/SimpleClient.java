/*
 * Copyright (C) 2014 Spencer Alderman
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.rogue.simpleclient;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * A simple class for launching authenticated Minecraft clients
 * <br /><br />
 * Made with MC release 1.7.9 in mind
 *
 * @since 1.0.0
 * @author 1Rogue
 * @version 1.0.0
 */
public final class SimpleClient {

    /** The minecraft version to launch */
    private final String version;
    /** The url for Mojang's authentication server */
    private final URL url = new URL("https://authserver.mojang.com/authenticate");
    /** The application data folder that contains minecraft */
    private final File appData;
    /** The minecraft directory */
    private final File minecraftDir;
    /** The game version directory */
    private final File gameDir;
    /** The natives folder for the version in use */
    private final File natives;
    /** Random client UUID used for two-factor client auth */
    private final UUID uuid = UUID.randomUUID();
    /** The response payload from the authentication server */
    private JSONObject response;

    /**
     * Constructs and authenticates a new client instance. Will ask for input
     * from a supplied {@link Scanner} source.
     *
     * @since 1.0.0
     * @version 1.0.0
     *
     * @param input The {@link Scanner} to read input from
     * @throws IOException Any type of communications failure with Mojang
     */
    public SimpleClient(Scanner input) throws IOException {
        System.out.print("What version are you launching (e.g. '1.7.9')?: ");
        this.version = input.nextLine();
        System.out.print("What is the full path for your application data folder containing minecraft?: ");
        this.appData = new File(input.nextLine());
        this.minecraftDir = new File(this.appData.getAbsoluteFile(),
                ".minecraft" + File.separatorChar);
        this.gameDir = new File(minecraftDir, "versions" + File.separator
                + this.version + File.separatorChar);
        this.natives = new File(gameDir, "natives" + File.separatorChar);
        System.out.print("Username: ");
        String username = input.nextLine();
        System.out.print("Password: ");
        this.connect(username, input.nextLine());
    }

    /**
     * Constructs and authenticates a new client instance. Will ask for input
     * from a supplied {@link Scanner} source.
     *
     * @since 1.0.0
     * @version 1.0.0
     *
     * @param version The minecraft version to use
     * @param username The username/email to authenticate with
     * @param password The password to authenticate with
     * @param appData The location of the application data folder containing MC
     * @throws IOException Any type of communications failure with Mojang
     */
    public SimpleClient(String version, String username, String password, File appData) throws IOException {
        this.version = version;
        this.appData = appData;
        this.minecraftDir = new File(this.appData.getAbsoluteFile(),
                ".minecraft" + File.separatorChar);
        this.gameDir = new File(minecraftDir, "versions" + File.separator
                + this.version + File.separatorChar);
        this.natives = new File(gameDir, "natives" + File.separatorChar);
        this.connect(username, password);
    }

    /**
     * Connects to and authenticates with the Mojang auth server
     *
     * @since 1.0.0
     * @version 1.0.0
     *
     * @param username The username/email to use
     * @param password The password to use
     * @throws IOException Incorrect credentials or some other connection error
     */
    private void connect(String username, String password) throws IOException {
        HttpURLConnection http = (HttpURLConnection) this.url.openConnection();
        http.setRequestMethod("POST");
        http.setRequestProperty("Content-Type", "application/json");
        http.setUseCaches(false);
        http.setDoOutput(true);
        try (DataOutputStream wr = new DataOutputStream(http.getOutputStream())) {
            wr.writeBytes(this.getPayload(username, password).toJSONString());
            wr.flush();
        }
        StringBuilder sb = new StringBuilder();
        try (InputStreamReader is = new InputStreamReader(http.getInputStream());
                BufferedReader br = new BufferedReader(is)) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        this.response = (JSONObject) JSONValue.parse(sb.toString());
    }

    /**
     * Constructs the JSON payload to send to Mojang's authentication server
     *
     * @since 1.0.0
     * @version 1.0.0
     *
     * @param username The username to use
     * @param password The password to use
     * @return A JSON payload for deployment to a webserver
     */
    private JSONObject getPayload(String username, String password) {
        JSONObject back = new JSONObject();
        JSONObject client = new JSONObject();
        client.put("name", "Minecraft");
        client.put("version", 1);
        back.put("agent", client);
        back.put("username", username);
        back.put("password", password);
        back.put("clientToken", this.uuid.toString());
        return back;
    }

    /**
     * Opens the minecraft client.
     *
     * @since 1.0.0
     * @version 1.0.0
     *
     * @throws SecurityException Lack of permissions to execute a process
     * @throws IOException Unobserved I/O Error
     */
    public void openMinecraft() throws SecurityException, IOException {
        MCProc mc = new MCProc(this.natives, this.minecraftDir, this.gameDir, this.version);
        JSONObject prof = (JSONObject) this.response.get("selectedProfile");
        mc.appendTag("username", (String) this.response.get("name"), "=");
        mc.appendTag("version", this.version, " ");
        mc.appendTag("gameDir", this.minecraftDir.getAbsolutePath(), " ");
        mc.appendTag("assetsDir", new File(this.minecraftDir, "assets").getAbsolutePath(), " ");
        mc.appendTag("userProperties", "{}", " ");
        mc.appendTag("accessToken", (String) this.response.get("accessToken"), " ");
        mc.appendTag("uuid", (String) prof.get("id"), " ");
        try {ProcessBuilder pb = new ProcessBuilder();
        pb.redirectErrorStream();
        pb.command(mc.getExec());
        Process p = Runtime.getRuntime().exec(mc.getExec());
        p.waitFor();
        BufferedReader bf = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = bf.readLine()) != null) {
            System.out.println(line);
        } } catch (Exception ex) { ex.printStackTrace(); }
    }

    /**
     * Helper class holding information about the execution process of the
     * Minecraft client.
     *
     * @since 1.0.0
     * @author 1Rogue
     * @version 1.0.0
     */
    private static class MCProc {

        /** Whether or not to print to console */
        private final boolean output = false;
        /** The raw, unfiltered executable statement */
        private String exec = "java -Djava.library.path=%NATIVES% "
                + "-cp %GAMEDIR%\\%VERSION%.jar;"
                + "%MCDIR%\\libraries\\com\\mojang\\authlib\\1.5.13\\authlib-1.5.13.jar;"
                + "%MCDIR%\\libraries\\java3d\\vecmath\\1.3.1\\vecmath-1.3.1.jar;"
                + "%MCDIR%\\libraries\\net\\sf\\trove4j\\trove4j\\3.0.3\\trove4j-3.0.3.jar;"
                + "%MCDIR%\\libraries\\com\\ibm\\icu\\icu4j-core-mojang\\51.2\\icu4j-core-mojang-51.2.jar;"
                + "%MCDIR%\\libraries\\net\\sf\\jopt-simple\\jopt-simple\\4.5\\jopt-simple-4.5.jar;"
                + "%MCDIR%\\libraries\\com\\paulscode\\codecjorbis\\20101023\\codecjorbis-20101023.jar;"
                + "%MCDIR%\\libraries\\com\\paulscode\\codecwav\\20101023\\codecwav-20101023.jar;"
                + "%MCDIR%\\libraries\\com\\paulscode\\libraryjavasound\\20101123\\libraryjavasound-20101123.jar;"
                + "%MCDIR%\\libraries\\com\\paulscode\\librarylwjglopenal\\20100824\\librarylwjglopenal-20100824.jar;"
                + "%MCDIR%\\libraries\\com\\paulscode\\soundsystem\\20120107\\soundsystem-20120107.jar;"
                + "%MCDIR%\\libraries\\io\\netty\\netty-all\\4.0.10.Final\\netty-all-4.0.10.Final.jar;"
                + "%MCDIR%\\libraries\\com\\google\\guava\\guava\\15.0\\guava-15.0.jar;"
                + "%MCDIR%\\libraries\\org\\apache\\commons\\commons-lang3\\3.1\\commons-lang3-3.1.jar;"
                + "%MCDIR%\\libraries\\commons-io\\commons-io\\2.4\\commons-io-2.4.jar;"
                + "%MCDIR%\\libraries\\net\\java\\jinput\\jinput\\2.0.5\\jinput-2.0.5.jar;"
                + "%MCDIR%\\libraries\\net\\java\\jutils\\jutils\\1.0.0\\jutils-1.0.0.jar;"
                + "%MCDIR%\\libraries\\com\\google\\code\\gson\\gson\\2.2.4\\gson-2.2.4.jar;"
                + "%MCDIR%\\libraries\\com\\mojang\\authlib\\1.2\\authlib-1.2.jar;"
                + "%MCDIR%\\libraries\\org\\apache\\logging\\log4j\\log4j-api\\2.0-beta9\\log4j-api-2.0-beta9.jar;"
                + "%MCDIR%\\libraries\\org\\apache\\logging\\log4j\\log4j-core\\2.0-beta9\\log4j-core-2.0-beta9.jar;"
                + "%MCDIR%\\libraries\\org\\lwjgl\\lwjgl\\lwjgl\\2.9.1\\lwjgl-2.9.1.jar;"
                + "%MCDIR%\\libraries\\org\\lwjgl\\lwjgl\\lwjgl_util\\2.9.1\\lwjgl_util-2.9.1.jar;"
                + "%MCDIR%\\libraries\\org\\lwjgl\\lwjgl\\lwjgl-platform\\2.9.1\\lwjgl-platform-2.9.1-natives-windows.jar;"
                + "%MCDIR%\\libraries\\net\\java\\jinput\\jinput-platform\\2.0.5\\jinput-platform-2.0.5-natives-windows.jar "
                + "net.minecraft.client.main.Main";

        /**
         * {@link MCProc} constructor
         * 
         * @since 1.0.0
         * @version 1.0.0
         * 
         * @param natives The natives folder location
         * @param mcdir Minecraft main folder location
         * @param gamedir The version directory
         * @param version The version of minecraft to use
         */
        public MCProc(File natives, File mcdir, File gamedir, String version) {
            this.out("exec: " + this.exec);
            this.exec = this.exec.replaceAll("\\%NATIVES\\%",
                    Matcher.quoteReplacement(natives.getAbsolutePath()));
            this.out("exec: " + this.exec);
            this.exec = this.exec.replaceAll("\\%GAMEDIR\\%",
                    Matcher.quoteReplacement(gamedir.getAbsolutePath()));
            this.out("exec: " + this.exec);
            this.exec = this.exec.replaceAll("\\%MCDIR\\%",
                    Matcher.quoteReplacement(mcdir.getAbsolutePath()));
            this.out("exec: " + this.exec);
            this.exec = this.exec.replaceAll("\\%VERSION\\%",
                    Matcher.quoteReplacement(version));
            this.out("exec: " + this.exec);
        }

        /**
         * Appends a varargs argument for the minecraft client
         * 
         * @since 1.0.0
         * @version 1.0.0
         * 
         * @param tag The tag key
         * @param value The tag value
         * @param delimiter Inbetween spacing, since some arguments differ
         */
        public void appendTag(String tag, String value, String delimiter) {
            this.exec += " --" + tag + delimiter + value;
        }

        /**
         * Returns an executable statement for {@link Runtime#exec(java.lang.String)}
         * 
         * @since 1.0.0
         * @version 1.0.0
         * 
         * @return A string representing an executable command for minecraft
         */
        public String getExec() {
            return this.exec;
        }

        /**
         * Returns a human-readable form of the executable command, new-line
         * delimited by a carat for use on a windows command-line environment
         * 
         * @since 1.0.0
         * @version 1.0.0
         * 
         * @return A readable executable statement
         */
        public String getReadableExec() {
            return this.getExec().replaceAll(Pattern.quote(";"), ";^\n");
        }

        /**
         * Prints out a string to stdout. Can be disabled
         * 
         * @since 1.0.0
         * @version 1.0.0
         * 
         * @param out The string to print
         */
        private void out(String out) {
            if (this.output) {
                System.out.println(out);
            }
        }

    }

}
