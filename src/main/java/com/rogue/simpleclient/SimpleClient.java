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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
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
        mc.appendTag("username", (String) prof.get("name"), "=");
        mc.appendTag("version", this.version, " ");
        mc.appendTag("gameDir", this.minecraftDir.getAbsolutePath(), " ");
        mc.appendTag("assetsDir", new File(this.minecraftDir, "assets").getAbsolutePath(), " ");
        mc.appendTag("userProperties", "{}", " ");
        mc.appendTag("accessToken", (String) this.response.get("accessToken"), " ");
        mc.appendTag("uuid", (String) prof.get("id"), " ");
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.redirectErrorStream(true);
            pb.command(mc.getExec());
            Process p = Runtime.getRuntime().exec(mc.getExec());
            p.waitFor();
            try (BufferedReader bf = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = bf.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
                + "-cp %LIBS%%GAMEDIR%" + File.separator + "%VERSION%.jar "
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
            this.exec = this.exec.replaceAll("\\%LIBS\\%",
                    Matcher.quoteReplacement(Library.getLibraryString(version)));
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

    private static enum Library {
        APACHE_COMMONS_3("org\\apache\\commons\\commons-lang3\\%s\\commons-lang3-%s", map("1.7.9", "3.1"), map("1.8.3", "3.3.2")),
        APACHE_COMMONS_COMPRESS("org\\apache\\commons\\commons-compress\\%s\\commons-compress-%s", map("1.8.3", "1.8.1")),
        ARGO("argo\\argo\\%s\\argo-%s", map("1.7.9", "2.25_fixed")),
        AUTHLIB("com\\mojang\\authlib\\%s\\authlib-%s", map("1.7.9", "1.5.13"), map("1.8.3", "1.5.17")),
        CODEC_JORBIS("com\\paulscode\\codecjorbis\\%s\\codecjorbis-%s", map("1.7.9", "20101023"), map("1.8.3", "20101023")),
        CODEC_WAV("com\\paulscode\\codecwav\\%s\\codecwav-%s", map("1.7.9", "20101023"), map("1.8.3", "20101023")),
        CODEC_COMMONS("commons-codec\\commons-codec\\%s\\commons-codec-%s", map("1.7.9", "1.9"), map("1.8.3", "1.9")),
        GSON("com\\google\\code\\gson\\gson\\%s\\gson-%s", map("1.7.9", "2.2.4"), map("1.8.3", "2.2.4")),
        GUAVA("com\\google\\guava\\guava\\%s\\guava-%s", map("1.7.9", "15.0"), map("1.8.3", "17.0")),
        HTTP_CORE("org\\apache\\httpcomponents\\httpcore\\%s\\httpcore-%s", map("1.8.3", "4.3.2")),
        HTTP_CLIENT("org\\apache\\httpcomponents\\httpclient\\%s\\httpclient-%s", map("1.8.3", "4.3.3")),
        ICU4J("com\\ibm\\icu\\icu4j-core-mojang\\%s\\icu4j-core-mojang-51.2", map("1.7.9", "51.2"), map("1.8.3", "51.2")),
        IO_COMMONS("commons-io\\commons-io\\%s\\commons-io-%s", map("1.7.9", "2.4"), map("1.8.3", "2.4")),
        JINPUT("net\\java\\jinput\\jinput\\%s\\jinput-%s", map("1.7.9", "2.0.5"), map("1.8.3", "2.0.5")),
        JINPUT_PLATFORM("net\\java\\jinput\\jinput-platform\\%s\\jinput-platform-%s-natives-windows", map("1.7.9", "2.0.5"), map("1.8.3", "2.0.5")),
        JNA("net\\java\\dev\\jna\\jna\\%s\\jna-%s", map("1.8.3", "3.4.0")),
        JNA_PLATFORM("net\\java\\dev\\jna\\platform\\%s\\platform-%s", map("1.8.3", "3.4.0")),
        JOPT_SIMPLE("net\\sf\\jopt-simple\\jopt-simple\\%s\\jopt-simple-%s", map("1.7.9", "4.5"), map("1.8.3", "4.6")),
        JUTILS("net\\java\\jutils\\jutils\\%s\\jutils-%s", map("1.7.9", "1.0.0"), map("1.8.3", "1.0.0")),
        LIBRARY_JAVASOUND("libraries\\com\\paulscode\\libraryjavasound\\%s\\libraryjavasound-%s", map("1.7.9", "20101123"), map("1.8.3", "20101123")),
        LIBRARY_LWJGL_OPENAL("com\\paulscode\\librarylwjglopenal\\%s\\librarylwjglopenal-%s", map("1.7.9", "20100824"), map("1.8.3", "20100824")),
        LOG4J_API("org\\apache\\logging\\log4j\\log4j-api\\%s\\log4j-api-%s", map("1.7.9", "2.0-beta9"), map("1.8.3", "2.0-beta9")),
        LOG4J_CORE("org\\apache\\logging\\log4j\\log4j-core\\%s\\log4j-core-%s", map("1.7.9", "2.0-beta9"), map("1.8.3", "2.0-beta9")),
        LOGGING_COMMONS("commons-logging\\commons-logging\\%s\\commons-logging-%s", map("1.8.3", "1.1.3")),
        LWJGL("libraries\\org\\lwjgl\\lwjgl\\lwjgl\\%s\\lwjgl-%s", map("1.7.9", "2.9.1"), map("1.8.3", "2.9.4-nightly-20150209")),
        LWJGL_UTIL("org\\lwjgl\\lwjgl\\lwjgl_util\\%s\\lwjgl_util-%s", map("1.7.9", "2.9.1"), map("1.8.3", "2.9.4-nightly-20150209")),
        LWJGL_PLATFORM("org\\lwjgl\\lwjgl\\lwjgl-platform\\%s\\lwjgl-platform-%s-natives-windows", map("1.7.9", "2.9.1"), map("1.8.3", "2.9.4-nightly-20150209")),
        NETTY_ALL("io\\netty\\netty-all\\%s\\netty-all-%s", map("1.7.9", "4.0.10.Final"), map("1.8.3", "4.0.23.Final")),
        OSHI("oshi-project\\oshi-core\\%s\\oshi-core-%s", map("1.8.3", "1.1")),
        REALMS("com\\mojang\\realms\\%s\\realms-%s", map("1.8.3", "1.7.13")),
        SOUND_SYSTEM("com\\paulscode\\soundsystem\\%s\\soundsystem-%s", map("1.7.9", "20120107"), map("1.8.3", "20120107")),
        TROVE4J("net\\sf\\trove4j\\trove4j\\%s\\trove4j-%s", map("1.7.9", "3.0.3")),
        TWITCH("tv\\twitch\\twitch\\%s\\twitch-%s", map("1.7.9", "5.16"), map("1.8.3", "6.5")),
        TWITCH_PLATFORM("tv\\twitch\\twitch-platform\\%s\\twitch-%s-natives-windows-" + arch(), map("1.7.9", "5.16"), map("1.8.3", "6.5")),
        TWITCH_EXTERNAL_PLATFORM("tv\\twitch\\twitch-external-platform\\%s\\twitch-%s-natives-windows-" + arch(), map("1.7.9", "5.16"), map("1.8.3", "6.5")),
        VECTOR_MATH("java3d\\vecmath\\%s\\vecmath-%s", map("1.7.9", "1.3.1")),
        ;

        private final String format;
        private final Map<String, String> vers;

        @SafeVarargs
        private Library(String format, Mapping<String, String>... vers) {
            this.format = "%%MCDIR%%" + File.separator + "libraries" + File.separator + format.replace("\\", File.separator) + ".jar;";
            this.vers = new HashMap<>(vers.length);
            for (Mapping<String, String> val : vers) {
                this.vers.put(val.getKey(), val.getValue());
            }
        }

        public Map<String, String> getLibraryVersionMappings() {
            return Collections.unmodifiableMap(this.vers);
        }
        
        public static Set<String> getMappedVersions(String version) {
            Set<String> back = new HashSet<>();
            for (Library lib : Library.values()) {
                if (lib.vers.containsKey(version)) {
                    String repl = lib.vers.get(version);
                    back.add(String.format(lib.format, repl, repl));
                }
            }
            return back;
        }
        
        public static String getLibraryString(String version) {
            StringBuilder sb = new StringBuilder();
            Library.getMappedVersions(version).forEach(sb::append);
            return sb.toString();
        }

        private static Mapping<String, String> map(String key, String val) {
            return new Mapping<>(key, val);
        }
        
        private static String arch() {
            boolean mojangUsesJVMArch = true;
            if (mojangUsesJVMArch) {
                return System.getProperty("os.arch").endsWith("64") ? "64" : "32";
            } else {
                String arch = System.getenv("PROCESSOR_ARCHITECTURE");
                String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
                return arch.endsWith("64")
                        || wow64Arch != null && wow64Arch.endsWith("64")
                        ? "64" : "32";
            }
        }
    }

    private static class Mapping<K, V> implements Map.Entry<K, V> {
        
        private final K key;
        private V value;

        public Mapping(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return this.key;
        }

        @Override
        public V getValue() {
            return this.value;
        }

        @Override
        public V setValue(V value) {
            V old = this.value;
            this.value = value;
            return old;
        }
    }

}
