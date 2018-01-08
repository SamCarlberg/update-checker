package com.github.samcarlberg.updatechecker;

import com.github.zafarkhaja.semver.Version;

import lombok.Value;

import java.net.URL;

/**
 * Represents a versioned Maven artifact.
 */
public @Value class Artifact {

  /**
   * The URL for the Maven repository containing this artifact.
   */
  URL mavenRepo;

  /**
   * The version of this artifact.
   */
  Version version;

}
