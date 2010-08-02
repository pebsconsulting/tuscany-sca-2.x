/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

package org.apache.tuscany.sca.shell;

import static java.lang.System.in;
import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import javax.xml.stream.XMLStreamException;

import org.apache.tuscany.sca.assembly.Composite;
import org.apache.tuscany.sca.common.java.io.IOHelper;
import org.apache.tuscany.sca.contribution.Artifact;
import org.apache.tuscany.sca.contribution.Contribution;
import org.apache.tuscany.sca.contribution.processor.ContributionReadException;
import org.apache.tuscany.sca.monitor.ValidationException;
import org.apache.tuscany.sca.node2.Node;
import org.apache.tuscany.sca.node2.NodeFactory;
import org.apache.tuscany.sca.runtime.ActivationException;
import org.apache.tuscany.sca.runtime.Version;
import org.apache.tuscany.sca.shell.jline.JLine;

/**
 * A little SCA command shell.
 */
public class Shell {
    
    public Node node;
    private boolean useJline;
    final List<String> history = new ArrayList<String>();
    private NodeFactory factory;
    public static final String[] COMMANDS = new String[] {"addDeploymentComposite", "help",
                                                   "install", "installed", 
                                                   "printDomainLevelComposite", 
                                                   "remove", "start", "status", "stop"};

    public static void main(final String[] args) throws Exception {
        boolean useJline = true;
        String domainURI = "default";
        for (String s : args) {
            if ("-nojline".equals(s)) {
                useJline = false;
            } else {
                domainURI = s;
            }
        }
        new Shell(domainURI, useJline).run();
    }

    public Shell(String domainURI, boolean useJLine) {
        this.factory = NodeFactory.newInstance();
        this.node = factory.createNode(domainURI);
        this.useJline = useJLine;
    }

    boolean addDeploymentComposite(final String curi, String contentURL) throws ContributionReadException, XMLStreamException, ActivationException, ValidationException, IOException {
        node.addDeploymentComposite(curi, new StringReader(readContents(contentURL)));
        return true;
    }

    boolean install(final List<String> toks) throws ContributionReadException, ActivationException, ValidationException {
        boolean runDeployables = !toks.contains("-norun");
        String metaDataURL = null;
        if (toks.contains("-metadata")) {
            metaDataURL = toks.get(toks.indexOf("-metadata")+1);
        }
        List<String> duris = null;
        if (toks.contains("-duris")) {
            duris = Arrays.asList(toks.get(toks.indexOf("-duris")+1).split(","));
        }

        String first = null;
        String second = null;
        for (int i=1; i<toks.size();i++) {
            if (toks.get(i).startsWith("-")) {
              if (!toks.get(i).equals("-norun")) {
                  i++;
              }
            } else {
                if (first == null) {
                    first = toks.get(i);
                } else {
                    second = toks.get(i);
                    break;
                }
            }
        }
        
        String curi = null;
        String curl = null;
        if (second != null) {
            curi = first;
            curl = second;
        } else {
            curl = first;
        }

        String uri = node.installContribution(curi, curl, metaDataURL, duris, runDeployables);
        out.println("installed at: " + uri);
        return true;
    }

    boolean installed(final List<String> toks) {
        List<String> curis;
        if (toks.size() > 1) {
            curis = Arrays.asList(new String[]{toks.get(1)});
        } else {
            curis =node.getInstalledContributions();
        }
        for (String curi : curis) {
            out.println(curi + " " + node.getInstalledContribution(curi).getLocation());
            Contribution c = node.getInstalledContribution(curi);
            for (Artifact a : c.getArtifacts()) {
                if (a.getModel() instanceof Composite) {
                    Composite composite = (Composite) a.getModel();
                    out.println("   " + composite.getURI() + " " + composite.getName());
                }
            }
        }
        return true;
    }
    boolean listComposites(final String curi) {
        Contribution c = node.getInstalledContribution(curi);
        for (Artifact a : c.getArtifacts()) {
            if (a.getModel() instanceof Composite) {
                out.println(((Composite)a.getModel()).getName());
            }
        }
        return true;
    }

    boolean printDomainLevelComposite() throws ContributionReadException, ActivationException, ValidationException {
        out.println("TODO");
        //out.println(node.getDomainLevelCompositeAsString());
        return true;
    }
    
    boolean getQNameDefinition(final String curi, String definintion, String symbolSpace) throws ContributionReadException, ActivationException, ValidationException {
        // TODO:
        return true;
    }

    boolean remove(final String curi) throws ContributionReadException, ActivationException, ValidationException {
        node.removeContribution(curi);
        return true;
    }

    public boolean stop(List<String> toks) throws ActivationException {
        if (toks == null || toks.size() < 2) {
            node.stop();
            factory.stop();
            return false;
        }
        String curi = toks.get(1);
        if (toks.size() > 2) {
            node.removeFromDomainLevelComposite(curi, toks.get(2));
        } else {
            for (String compositeURI : node.getDeployedCompostes(curi)) {
                node.removeFromDomainLevelComposite(curi, compositeURI);
            }
        }

        return true;
    }

    boolean start(String curi, String compositeURI) throws ActivationException, ValidationException {
        node.addToDomainLevelComposite(curi, compositeURI);
        return true;
    }
    
    boolean status(final List<String> toks) {
        out.println("Domain: " + node.getDomainName());
        List<String> ics;
        if (toks.size()>1) {
            ics = new ArrayList<String>();
            ics.add(toks.get(1));
        } else {
            ics = node.getInstalledContributions();
        }

        for (String curi : ics) {
            Contribution c = node.getInstalledContribution(curi);
            List<String> dcs = node.getDeployedCompostes(curi);
            if (toks.size()>2) {
                dcs = new ArrayList<String>();
                dcs.add(toks.get(2));
            } else {
                dcs = node.getDeployedCompostes(curi);
            }
            for (String compositeUri : dcs) {
                for (Artifact a : c.getArtifacts()) {
                    if (compositeUri.equals(a.getURI())) {
                        out.println("   " + curi + " " + compositeUri + " " + ((Composite)a.getModel()).getName());
                    }
                }
            }
        }
        return true;
    }

    boolean history() {
        for (String l: history)
            out.println(l);
        return true;
    }
    
    List<String> read(Object r) throws IOException {
        out.print("=> ");
        final String l;
        if (useJline) {
            l = JLine.readLine(r);
        } else {
            l = ((BufferedReader)r).readLine();
            history.add(l);
        }
        String[] toks = l != null? l.trim().split(" ") : "stop".split(" ");
        List<String> toksList = new ArrayList<String>();
        for (String s : toks) {
            if (s != null && s.trim().length()>0) {
                toksList.add(s);
            }
        }
        return toksList;
    }

    Callable<Boolean> eval(final List<String> toks) {
        final String op = toks.size() > 0 ? toks.get(0) : "";

        if (op.equalsIgnoreCase("addDeploymentComposite")) return new Callable<Boolean>() { public Boolean call() throws Exception {
            return addDeploymentComposite(toks.get(1), toks.get(2));
        }};
        if (op.equalsIgnoreCase("install")) return new Callable<Boolean>() { public Boolean call() throws Exception {
            return install(toks);
        }};
        if (op.equalsIgnoreCase("installed")) return new Callable<Boolean>() { public Boolean call() throws Exception {
            return installed(toks);
        }};
        if (op.equalsIgnoreCase("printDomainLevelComposite")) return new Callable<Boolean>() { public Boolean call() throws Exception {
            return printDomainLevelComposite();
        }};
        if (op.equalsIgnoreCase("getQNameDefinition")) return new Callable<Boolean>() { public Boolean call() throws Exception {
            return getQNameDefinition(toks.get(1), toks.get(2), toks.get(3));
        }};
        if (op.equalsIgnoreCase("remove")) return new Callable<Boolean>() { public Boolean call() throws Exception {
            return remove(toks.get(1));
        }};
        if (op.equalsIgnoreCase("help")) return new Callable<Boolean>() { public Boolean call() {
            return help(toks);
        }};
        if (op.equalsIgnoreCase("stop")) return new Callable<Boolean>() { public Boolean call() throws Exception {
            return stop(toks);
        }};
        if (op.equalsIgnoreCase("bye")) return new Callable<Boolean>() { public Boolean call() throws Exception {
            return stop(null);
        }};
        if (op.equalsIgnoreCase("start")) return new Callable<Boolean>() { public Boolean call() throws Exception {
            return start(toks.get(1), toks.get(2));
        }};
        if (op.equalsIgnoreCase("status")) return new Callable<Boolean>() { public Boolean call() {
            return status(toks);
        }};
        if (op.equalsIgnoreCase("history")) return new Callable<Boolean>() { public Boolean call() {
            return history();
        }};
        if (op.equalsIgnoreCase("")) return new Callable<Boolean>() { public Boolean call() {
            return true;
        }};
        return new Callable<Boolean>() { public Boolean call() {
            out.println("unknown command");
            return true;
        }};
    }

    boolean apply(final Callable<Boolean> func) {
        try {
            return func.call();
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public void run() throws IOException {
        help(null);
        Object reader;
        if (useJline) {
            reader = JLine.createJLineReader(this);
        } else {
            reader = new BufferedReader(new InputStreamReader(in));
        }
        while(apply(eval(read(reader))));
    }

    String readContents(String location) throws IOException {
        URL url = IOHelper.getLocationAsURL(location);
        InputStream is = IOHelper.openStream(url);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder builder = new StringBuilder(8192);
            for(String line=br.readLine(); line!=null; line=br.readLine()) {
                builder.append(line);
                builder.append('\n');
            }
            return builder.toString();
        } finally {
            IOHelper.close(is);
        }
     }
    
    boolean help(List<String> toks) {
        String command = (toks == null || toks.size() < 2) ? null : toks.get(1);
        if (command == null) {
            helpOverview();
        } else if ("help".equalsIgnoreCase(command)){
            helpHelp();
        } else if ("install".equalsIgnoreCase(command)) {
            helpInstall();
        } else if ("installed".equalsIgnoreCase(command)) {
            helpInstalled();
        } else if ("remove".equalsIgnoreCase(command)) {
            helpRemove();
        } else if ("addDeploymentComposite".equalsIgnoreCase(command)) {
            helpAddDeploymentComposite();
        } else if ("printDomainLevelComposite".equalsIgnoreCase(command)) {
            helpPrintDomainLevelComposite();
        } else if ("start".equalsIgnoreCase(command)) {
            helpStart();
        } else if ("status".equalsIgnoreCase(command)) {
            helpStatus();
        } else if ("stop".equalsIgnoreCase(command)) {
            helpStop();
        } else if ("startup".equalsIgnoreCase(command)){
            helpStartUp();
        }
        return true;
    }

    boolean helpOverview() {
        out.println("Apache Tuscany Shell (" + Version.getVersion() + " " + Version.getRevsion() + " " + Version.getBuildTime() + ")");
        out.println("Commands:");
        out.println();
        out.println("   help");
        out.println("   install [<uri>] <contributionURL> [-norun -metadata <url> -duris <uri,uri,...>]");
        out.println("   installed [<contributionURI>]");
        out.println("   remove <contributionURI>");
        out.println("   addDeploymentComposite <contributionURI> <contentURL>");
        out.println("   printDomainLevelComposite");
        out.println("   start <curi> <compositeUri>");
        out.println("   status [<curi> <compositeUri>]");
        out.println("   stop [<curi> <compositeUri>]");
        out.println();
        if (useJline) out.println("Use Tab key for command and argument completion");
        out.println("For detailed help on each command do 'help <command>', for help of startup options do 'help startup'");
        out.println();
        return true;
    }
    void helpHelp() {
        out.println("   help [<command>]");
        out.println();
        out.println("   Outputs help on the Tuscany Shell");
        out.println("   If the command argument is used it provides detailed help on that command otherwise");
        out.println("   it provides an overview of available Shell commands");
        out.println();
        out.println("   To get help on starting the Tuscany Shell use 'help startup'");
        out.println();
        out.println("   Arguments:");
        out.println("      <command> - (optional) the command to get detailed help on");
    }

    void helpAddDeploymentComposite() {
        out.println("   addDeploymentComposite <contributionURI> <contentURL>");
        out.println();
        out.println("   Adds a deployment composite using a supplied composite ('composite by value' - a data");
        out.println("   structure, not an existing resource in the Domain) to the contribution identified by a"); 
        out.println("   supplied contribution URI. The added deployment composite is given a relative URI that");
        out.println("   matches the @name attribute of the composite, with a '.composite' suffix. Since all composites"); 
        out.println("   run within the context of an installed contribution (any component implementations or other");
        out.println("   definitions are resolved within that contribution), this functionality makes it possible");
        out.println("   for the deployer to create a composite with final configuration and wiring decisions and add");  
        out.println("   it to an installed contribution without having to modify the contents of the root contribution.");
        out.println();
        out.println("   Arguments:");
        out.println("      <contributionURI> - (required) the URI of an installed contribution");
        out.println("      <contentURL> - (required) the location of the composite");
    }

    void helpInstall() {
        out.println("   install [<uri>] <contributionURL> [-norun -metadata <url> -duris <uri,uri,...>]");
        out.println();
        out.println("   Creates an installed contribution with a supplied root contribution, installed at abase URI.");
        out.println();
        out.println("   Arguments:");
        out.println("      uri - (optional) the URI (name) to use for the contribution. When no uri is specified");
        out.println("               a default URI is used derived from the contribution URL");
        out.println("      contributionURL - (required) the URL to the contribution to install");
        out.println("      -norun - (optional) do not start any composites listed as deployable in the sca-contribution.xml file");
        out.println("      -metadata <url> - (optional) the URL to an external contribution meta data document that should be");
        out.println("               merged into any existing sca-contributions.xml file within the contribution.");
        out.println("      -duris <uri,uri,...> - (optional) specifies the URIs of contributions that are used to resolve the");
        out.println("               dependencies of the root contribution and other dependent contributions.");
        out.println("               When not specified all installed contributions are used to resolve dependencies.");
    }

    void helpInstalled() {
        out.println("   installed [<contributionURI>]");
        out.println();
        out.println("   Shows information about the contributions installed on this node,");
        out.println("   including the contribution URI and location along with the URI");
        out.println("   and QName of any composites within the contribution");
        out.println();
        out.println("   Arguments:");
        out.println("      contributionURI - (optional) the URI of an installed contribution");
    }

    void helpRemove() {
        out.println("   remove <contributionURI>");
        out.println();
        out.println("   Removes an installed contribution");
        out.println();
        out.println("   Arguments:");
        out.println("      contributionURI - (required) the URI of an installed contribution");
    }

    void helpPrintDomainLevelComposite() {
        out.println("   printDomainLevelComposite");
        out.println();
        out.println("   Not yet implemented");
        out.println();
        out.println("   Arguments:");
        out.println("      none");
    }

    void helpStart() {
        out.println("   start <curi> <compositeUri>");
        out.println();
        out.println("   Starts a composite.");
        out.println("   The composite is added to the domain composite with semantics that correspond to the domain-level");
        out.println("   composite having an <include> statement that references the supplied composite. All of the composites");
        out.println("   components become top-level components and the component services become externally visible");
        out.println("   services (eg. they would be present in a WSDL description of the Domain).");
        out.println();
        out.println("   Arguments:");
        out.println("      curi - (required) the URI of an installed contribution");
        out.println("      compositeUri - (required) the URI of a composite");
    }

    void helpStatus() {
        out.println("   status [<curi> <compositeUri>]");
        out.println();
        out.println("   Shows the status of the Node, listing for each deployed composite its");
        out.println("   contribution URI, the composite URI, and the composite QName.");
        out.println();
        out.println("   Arguments:");
        out.println("      curi - (optional) the URI of an installed contribution");
        out.println("      compositeUri - (optional) the URI of a composite");
    }

    void helpStop() {
        out.println("   stop [<curi> <compositeUri>]");
        out.println();
        out.println("   Stops this Node or individual composites and contributions in the Node.");
        out.println("   If a composite URI is specified then the composite is removed from the Domain Level composite");
        out.println("   This means that the removal of the components, wires, services and references originally added");
        out.println("   to the domain level composite by the identified composite. If a contribution URI is specified");
        out.println("   without a composite URI then all deployed composites composites in the contribution are stopped.");
        out.println("   If no contribution URI is specified then the entire Node is stopped and the Shell exits.");
        out.println();
        out.println("   Arguments:");
        out.println("      curi - (optional) the URI of an installed contribution");
        out.println("      compositeUri - (optional) the URI of a composite");
    }

    void helpStartUp() {
        out.println("   Tuscany Shell StartUp Options ");
        out.println();
        out.println("   When starting the Tuscany Shell there are optional arguments that can configure the Shell.");
        out.println();
        out.println("   Arguments:");
        out.println("      <domainURI> (optional) the URI of the domain.");
        out.println("                  When the domainURI is a simple string then the Shell starts a standalone");
        out.println("                  Node using the string as the domain name or 'default' if no name is specified.");
        out.println("                  When the domainURI starts with 'uri:' the Shell starts a distributed Node ");
        out.println("                  and the URI can encode parameters to configure the domain as follows:");
        out.println("                  uri:<domainName?key1=value1&key2=value2&...");
        out.println("                  The keys are optional and some keys are:");
        out.println("                  bind=ip[:port] - defines the local bind address and port, if the port is not specified it");
        out.println("                      defaults 14820 and if that port in use it will try incrementing by one till a free port is found.");
        out.println("                  multicast=groupip:port | off - defines if multicast discovery is used and if so what multicast IP group and port is used.");
        out.println("                      It defaults to 224.5.12.10:51482. A value of 'off' means multicast is disabled.");
        out.println("                  wka=ip[:port] - a comma separated list of ip address and port for remote nodes in");
        out.println("                                  the domain group when multicast is not available. The port defaults to 14820.");
        out.println("                  userid= is the userid other nodes must use to connect to this domain group. The default is the default domain name.");
        out.println("                  password= is the password other nodes must use to connect to this domain group. The default is 'tuscany'.");
        out.println();
        out.println("      -nojline    (optional) use plain Java System.in/out instead of JLine");
        out.println("                             (no tab completion or advanced line editing will be available)");
    }
}
