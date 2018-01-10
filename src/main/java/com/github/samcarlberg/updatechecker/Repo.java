package com.github.samcarlberg.updatechecker;

import com.github.zafarkhaja.semver.Version;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Optional;

public interface Repo {

  /**
   * Gets the name of this repo.
   */
  String getName();

  /**
   * Gets the location of this repo.
   */
  URL getLocation();

  /**
   * Gets all the available versions of the software with with given group ID and artifact name.
   * If no such software exists, the returned collection is empty.
   *
   * @param group    the group ID of the software, in whatever format is appropriate for this repo
   * @param artifact the name of the software artifact
   *
   * @throws IOException if the repository could not be read from
   */
  Collection<Version> getAvailableVersions(String group, String artifact) throws IOException;

  /**
   * Gets the most recent version of the software with the given group ID and artifact name on
   * this repo.
   *
   * @param group    the group ID of the software
   * @param artifact the name of the software artifact
   *
   * @throws IOException if the repository could not be read from
   */
  Optional<Version> getMostRecentVersion(String group, String artifact) throws IOException;

  /**
   * Creates a new Maven repository.
   *
   * @param name     the name of the Maven repository
   * @param location the location of the Maven repository
   */
  static Repo maven(String name, URL location) {
    return new MavenRepo(name, location);
  }

}
