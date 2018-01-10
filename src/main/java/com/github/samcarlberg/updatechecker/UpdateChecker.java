package com.github.samcarlberg.updatechecker;

import com.github.zafarkhaja.semver.Version;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Checks for updates to versioned software packages on arbitrary Maven servers.
 */
public final class UpdateChecker {

  private static final Logger log = Logger.getLogger(UpdateChecker.class.getName());

  private final String groupId;
  private final String artifactName;
  private final String version;
  private final Set<Repo> repos = new LinkedHashSet<>();

  private UpdateStatus status = null;

  /**
   * Creates a new update checker.
   *
   * @param groupId      the group ID of the software package
   * @param artifactName the name of the software artifact
   * @param version      the current version of the software
   */
  public UpdateChecker(String groupId, String artifactName, String version) {
    this.groupId = groupId;
    this.artifactName = artifactName;
    this.version = version;
  }

  public UpdateChecker usingRepos(Repo... repos) {
    Collections.addAll(this.repos, repos);
    return this;
  }

  public static UpdateChecker create(String groupId, String artifactName, String version, Repo... repos) {
    return new UpdateChecker(groupId, artifactName, version).usingRepos(repos);
  }

  /**
   * Gets a list of all the available versions on all repositories.
   *
   * @throws IOException if any of the repositories could not be read
   */
  public List<Version> getVersions() throws IOException {
    Collection<Version> versions = new LinkedHashSet<>();
    for (Repo repo : repos) {
      versions.addAll(repo.getAvailableVersions(groupId, artifactName));
    }
    return versions
        .stream()
        .sorted(Version::compareWithBuildsTo)
        .collect(Collectors.toList());
  }

  /**
   * A "safe" version of {@link #getVersions()} that does not throw an exception.
   */
  public List<Version> getVersionsSafe() {
    try {
      return getVersions();
    } catch (IOException e) {
      return Collections.emptyList();
    }
  }

  /**
   * Gets the most recent version of the software on any of the repositories.
   */
  public Optional<Version> getMostRecentVersion() throws IOException {
    return getVersions().stream().max(Version::compareWithBuildsTo);
  }

  /**
   * A "safe" version of {@link #getMostRecentVersion()} that does not throw an exception.
   */
  public Optional<Version> getMostRecentVersionSafe() {
    try {
      return getMostRecentVersion();
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  /**
   * Gets the most recent artifact (URL + version) of the software. If multiple remotes have the
   * most recent version, the artifact may point to any one of them.
   */
  public Optional<Artifact> getMostRecentArtifact() throws IOException {
    Collection<Artifact> mostRecent = new LinkedHashSet<>();
    for (Repo repo : repos) {
      repo.getMostRecentVersion(groupId, artifactName)
          .map(v -> Artifact.create(repo, groupId, artifactName, v))
          .ifPresent(mostRecent::add);
    }
    return mostRecent.stream()
        .max(Comparator.comparing(Artifact::getVersion, Version::compareWithBuildsTo));
  }

  /**
   * A "safe" version of {@link #getMostRecentArtifact()} that does not throw an exception.
   */
  public Optional<Artifact> getMostRecentArtifactSafe() {
    try {
      return getMostRecentArtifact();
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  /**
   * Gets the location of the JAR file for the most recent artifact.
   */
  public Optional<URL> getMostRecentArtifactLocation() throws IOException {
    Optional<Artifact> mostRecentArtifact = getMostRecentArtifact();
    if (mostRecentArtifact.isPresent()) {
      return Optional.of(mostRecentArtifact.get().getJarLocation());
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
      if (availableVersions.isEmpty()) {
        return UpdateStatus.UNKNOWN;
      }
      return availableVersions.stream().anyMatch(v -> v.greaterThan(current))
          ? UpdateStatus.OUTDATED
          : UpdateStatus.UP_TO_DATE;
    } catch (IOException e) {
      log.log(Level.SEVERE, "Could not read from Maven repositories", e);
      return UpdateStatus.UNKNOWN;
    }
  }

}
