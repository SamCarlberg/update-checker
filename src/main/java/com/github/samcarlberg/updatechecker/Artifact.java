package com.github.samcarlberg.updatechecker;

import com.github.zafarkhaja.semver.Version;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Represents a versioned artifact.
 */
@Value
@AllArgsConstructor(staticName = "create")
public class Artifact {

  /**
   * The repository containing this artifact.
   */
  Repo repo;

  String groupId;

  String name;

  /**
   * The version of this artifact.
   */
  Version version;

  public URL getJarLocation() throws MalformedURLException {
    return new URL(repo.getLocation()
        + groupId.replace('.', '/')
        + '/' + name
        + '/' + version
        + '/' + name
        + '-' + version
        + ".jar");
  }

}
