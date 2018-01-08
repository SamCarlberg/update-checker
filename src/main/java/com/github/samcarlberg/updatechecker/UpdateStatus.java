package com.github.samcarlberg.updatechecker;

/**
 * Represents the update status of a versioned library or application.
 */
public enum UpdateStatus {

  /**
   * The software is up-to-date; no more recent version was discovered on any server.
   */
  UP_TO_DATE,

  /**
   * The software is outdated; at least one release has been made since the current version in use.
   */
  OUTDATED,

  /**
   * No versioning information could be found.
   */
  UNKNOWN,

}
