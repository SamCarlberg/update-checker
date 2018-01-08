package com.github.samcarlberg.updatechecker;

import com.github.zafarkhaja.semver.Version;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Checks for updates to versioned software packages on arbitrary Maven servers.
 */
public class UpdateChecker {

  private static final Logger log = Logger.getLogger(UpdateChecker.class.getName());

  private final String groupId;
  private final String artifactName;
  private final String version;
  private final String classifier;
  private final Set<URL> mavenCoordinates = new LinkedHashSet<>();
  private final Set<Artifact> artifacts = new LinkedHashSet<>();
  private boolean checkRemotes = true;

  private UpdateStatus status = null;

  private static final Pattern versionPattern = Pattern.compile("^\\s*<version>(.+)</version>\\s*$");

  /**
   * Creates a new update checker.
   *
   * @param groupId      the group ID of the software package
   * @param artifactName the name of the software artifact
   * @param version      the current version of the software
   * @param classifier   an optional classifier to the version
   */
  public UpdateChecker(String groupId, String artifactName, String version, String classifier) {
    this.groupId = groupId;
    this.artifactName = artifactName;
    this.version = version;
    this.classifier = classifier;
  }

  /**
   * Creates a new update checker for software without a classifier.
   *
   * @param groupId      the group ID of the software package
   * @param artifactName the name of the software artifact
   * @param version      the current version of the software
   */
  public UpdateChecker(String groupId, String artifactName, String version) {
    this(groupId, artifactName, version, null);
  }

  /**
   * Sets Maven repositories to look in. This may be called multiple times.
   *
   * @param repos repositories to search
   *
   * @return this checker
   *
   * @throws MalformedURLException if any of the repos are malformed URLs
   */
  public UpdateChecker useMavenRepos(String... repos) throws MalformedURLException {
    for (String repo : repos) {
      mavenCoordinates.add(new URL(repo));
    }
    return this;
  }

  /**
   * Sets Maven repositories to look in. This may be called multiple times.
   *
   * @param repos repositories to search
   *
   * @return this checker
   */
  public UpdateChecker useMavenRepos(URL... repos) {
    Collections.addAll(mavenCoordinates, repos);
    return this;
  }

  /**
   * Sets this checker to read from the remotes after the first use.
   */
  public void checkRemotesAgain() {
    checkRemotes = true;
    status = null;
  }

  /**
   * Gets a list of all the available versions on all Maven repositories.
   *
   * @throws IOException if any of the repositories could not be read
   */
  public List<Version> getVersions() throws IOException {
    List<Version> versions = new ArrayList<>();
    for (URL url : mavenCoordinates) {
      getVersions(url)
          .filter(v -> !versions.contains(v))
          .forEachOrdered(versions::add);
    }
    checkRemotes = false;
    return versions
        .stream()
        .sorted(Version::compareWithBuildsTo)
        .collect(Collectors.toList());
  }

  private Stream<Version> getVersions(URL url) throws IOException {
    if (!checkRemotes) {
      return artifacts.stream()
          .filter(a -> a.getMavenRepo().equals(url))
          .map(Artifact::getVersion);
    }
    URI mavenMetadataUri = getMavenMetadataUri(url);
    if (log.isLoggable(Level.FINER)) {
      log.finer("Getting Maven version info from " + mavenMetadataUri);
    }
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(mavenMetadataUri.toURL().openStream()))) {
      List<Version> versions = reader.lines()
          .map(versionPattern::matcher)
          .filter(Matcher::matches)
          .map(m -> m.group(1))
          .map(Version::valueOf)
          .collect(Collectors.toList());
      versions.stream().map(v -> new Artifact(url, v))
          .forEach(artifacts::add);
      return versions.stream();
    }
  }

  private URI getMavenMetadataUri(URL mavenRepo) {
    return URI.create(
        mavenRepo.toString()
            + '/' + groupId.replace('.', '/')
            + '/' + artifactName
            + "/maven-metadata.xml"
    );
  }

  /**
   * Gets the most recent version of the software on any of the repositories.
   */
  public Optional<Version> getMostRecentVersion() {
    try {
      List<Version> versions = getVersions();
      return versions.stream().max(Version::compareWithBuildsTo);
    } catch (IOException e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  /**
   * Gets the most recent artifact (URL + version) of the software. If multiple remotes have the
   * most recent version, the artifact may point to any one of them.
   */
  public Optional<Artifact> getMostRecentArtifact() {
    return artifacts.stream()
        .max((a, b) -> a.getVersion().compareWithBuildsTo(b.getVersion()));
  }

  /**
   * @return
   */
  public List<Artifact> getAllDiscoveredArtifacts() {
    return new ArrayList<>(artifacts);
  }

  /**
   * Gets the location of the JAR file for the most recent artifact.
   */
  public Optional<URI> getMostRecentArtifactLocation() {
    Optional<Artifact> mostRecentArtifact = getMostRecentArtifact();
    if (mostRecentArtifact.isPresent()) {
      Artifact artifact = mostRecentArtifact.get();
      Version version = artifact.getVersion();
      String start = artifact.getMavenRepo().toExternalForm() + toRelativePath(groupId, artifactName)
          + "/" + version
          + "/" + artifactName + "-" + version;
      if (classifier != null && !classifier.isEmpty()) {
        start += "-" + classifier;
      }
      return Optional.of(URI.create(start + ".jar"));
    }
    return Optional.empty();
  }

  /**
   * Gets the update status of the software.
   */
  public UpdateStatus getStatus() {
    if (status == null) {
      status = computeStatus();
    }
    return status;
  }

  private UpdateStatus computeStatus() {
    Version current = Version.valueOf(version);
    try {
      List<Version> availableVersions = getVersions();
      return availableVersions.stream().anyMatch(v -> v.greaterThan(current))
          ? UpdateStatus.OUTDATED
          : UpdateStatus.UP_TO_DATE;
    } catch (IOException e) {
      log.log(Level.SEVERE, "Could not read from Maven repositories", e);
      return UpdateStatus.UNKNOWN;
    }
  }

  private static String toRelativePath(String groupId, String artifactName) {
    return String.format("/%s/%s", groupId.replace('.', '/'), artifactName);
  }

}
