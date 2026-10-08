package com.jackson.migration.rewrite;

import jakarta.enterprise.context.ApplicationScoped;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates a temporary Maven user-settings file that preserves the developer's existing Maven
 * settings while activating the repository required by current OpenRewrite releases.
 *
 * <p>The generated file is temporary and must be deleted by the caller. The target application's
 * {@code pom.xml} is never modified merely to make the OpenRewrite plugin resolvable.</p>
 */
@ApplicationScoped
public class RewriteMavenSettingsService {
    static final String PROFILE_ID = "mta-migration-tool-openrewrite";

    /**
     * Builds temporary Maven settings containing both normal and plugin repository entries.
     *
     * @param repositoryUrl OpenRewrite artifact repository URL
     * @return temporary Maven settings file
     * @throws IOException when settings cannot be read or written
     */
    public Path create(String repositoryUrl) throws IOException {
        return create(repositoryUrl, System.getenv("CODE_GENOME_USERNAME"), System.getenv("CODE_GENOME_TOKEN"));
    }

    /**
     * Builds temporary Maven settings using explicitly supplied Code Genome credentials when the
     * developer's existing settings do not already define a {@code codegenome} server.
     *
     * @param repositoryUrl OpenRewrite artifact repository URL
     * @param username Code Genome username/email; may be blank when existing settings provide credentials
     * @param token Code Genome download token; may be blank when existing settings provide credentials
     * @return temporary Maven settings file
     * @throws IOException when credentials are unavailable or settings cannot be read/written
     */
    Path create(String repositoryUrl, String username, String token) throws IOException {
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            throw new IOException("OpenRewrite repository URL is not configured");
        }

        try {
            Path existing = Path.of(System.getProperty("user.home"), ".m2", "settings.xml");
            Document document;
            Element settings;

            DocumentBuilderFactory factory = secureFactory();
            if (Files.isRegularFile(existing)) {
                document = factory.newDocumentBuilder().parse(existing.toFile());
                settings = document.getDocumentElement();
            } else {
                document = factory.newDocumentBuilder().newDocument();
                settings = document.createElementNS("http://maven.apache.org/SETTINGS/1.0.0", "settings");
                settings.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", "http://maven.apache.org/SETTINGS/1.0.0");
                settings.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                settings.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation",
                        "http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd");
                document.appendChild(settings);
            }

            String ns = settings.getNamespaceURI();
            ensureCodeGenomeCredentials(document, ns, settings, username, token);

            Element profiles = directChild(settings, "profiles");
            if (profiles == null) {
                profiles = element(document, ns, "profiles");
                settings.appendChild(profiles);
            }

            Element profile = profileById(profiles, PROFILE_ID);
            if (profile == null) {
                profile = element(document, ns, "profile");
                appendText(document, ns, profile, "id", PROFILE_ID);
                profiles.appendChild(profile);
            }

            ensureRepository(document, ns, profile, "repositories", repositoryUrl);
            ensureRepository(document, ns, profile, "pluginRepositories", repositoryUrl);

            Element activeProfiles = directChild(settings, "activeProfiles");
            if (activeProfiles == null) {
                activeProfiles = element(document, ns, "activeProfiles");
                settings.appendChild(activeProfiles);
            }
            if (!hasDirectChildText(activeProfiles, "activeProfile", PROFILE_ID)) {
                appendText(document, ns, activeProfiles, "activeProfile", PROFILE_ID);
            }

            Path temp = Files.createTempFile("mta-migration-tool-maven-settings-", ".xml");
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(document), new StreamResult(temp.toFile()));
            return temp;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Unable to prepare temporary Maven settings for OpenRewrite", e);
        }
    }

    private static DocumentBuilderFactory secureFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private static void ensureRepository(Document document, String ns, Element profile, String containerName,
                                         String repositoryUrl) {
        Element container = directChild(profile, containerName);
        if (container == null) {
            container = element(document, ns, containerName);
            profile.appendChild(container);
        }
        if (repositoryById(container, "codegenome") != null) return;

        String itemName = "pluginRepositories".equals(containerName) ? "pluginRepository" : "repository";
        Element repository = element(document, ns, itemName);
        appendText(document, ns, repository, "id", "codegenome");
        appendText(document, ns, repository, "url", repositoryUrl);

        Element releases = element(document, ns, "releases");
        appendText(document, ns, releases, "enabled", "true");
        repository.appendChild(releases);

        Element snapshots = element(document, ns, "snapshots");
        appendText(document, ns, snapshots, "enabled", "false");
        repository.appendChild(snapshots);
        container.appendChild(repository);
    }


    private static void ensureCodeGenomeCredentials(Document document, String ns, Element settings,
                                                     String username, String token) throws IOException {
        Element servers = directChild(settings, "servers");
        if (servers != null && serverById(servers, "codegenome") != null) return;

        if (username == null || username.isBlank() || token == null || token.isBlank()) {
            throw new IOException(
                    "OpenRewrite Code Genome credentials are required. Configure a <server> with id " +
                    "'codegenome' in ~/.m2/settings.xml, or set CODE_GENOME_USERNAME and " +
                    "CODE_GENOME_TOKEN before running migrate resume.");
        }

        if (servers == null) {
            servers = element(document, ns, "servers");
            settings.appendChild(servers);
        }
        Element server = element(document, ns, "server");
        appendText(document, ns, server, "id", "codegenome");
        appendText(document, ns, server, "username", username);
        appendText(document, ns, server, "password", token);
        servers.appendChild(server);
    }

    private static Element serverById(Element servers, String id) {
        for (Element child : directChildren(servers, "server")) {
            if (hasDirectChildText(child, "id", id)) return child;
        }
        return null;
    }

    private static Element profileById(Element profiles, String id) {
        for (Element child : directChildren(profiles, "profile")) {
            if (hasDirectChildText(child, "id", id)) return child;
        }
        return null;
    }

    private static Element repositoryById(Element repositories, String id) {
        for (Element child : directChildren(repositories, null)) {
            if (hasDirectChildText(child, "id", id)) return child;
        }
        return null;
    }

    private static boolean hasDirectChildText(Element parent, String name, String value) {
        Element child = directChild(parent, name);
        return child != null && value.equals(child.getTextContent().trim());
    }

    private static Element directChild(Element parent, String localName) {
        for (Element child : directChildren(parent, localName)) return child;
        return null;
    }

    private static java.util.List<Element> directChildren(Element parent, String localName) {
        java.util.List<Element> result = new java.util.ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element element = (Element) node;
            String actual = element.getLocalName() != null ? element.getLocalName() : element.getNodeName();
            if (localName == null || localName.equals(actual)) result.add(element);
        }
        return result;
    }

    private static Element element(Document document, String ns, String name) {
        return ns == null ? document.createElement(name) : document.createElementNS(ns, name);
    }

    private static void appendText(Document document, String ns, Element parent, String name, String value) {
        Element child = element(document, ns, name);
        child.setTextContent(value);
        parent.appendChild(child);
    }
}
