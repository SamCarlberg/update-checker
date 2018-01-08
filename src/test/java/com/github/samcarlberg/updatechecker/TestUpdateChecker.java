package com.github.samcarlberg.updatechecker;

import com.github.zafarkhaja.semver.Version;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUpdateChecker {

  private static final String testRepo = TestUpdateChecker.class.getResource("/")
      .toExternalForm()
      .replace("classes", "resources");

  @Test
  public void testOutdated() throws MalformedURLException {
    UpdateChecker checker = new UpdateChecker("foo", "bar", "0.4.0");
    checker.useMavenRepos(testRepo);
    assertEquals(UpdateStatus.OUTDATED, checker.getStatus(), "Unexpected status");
    assertEquals(Version.valueOf("1.0.0"), checker.getMostRecentVersion().get());
    assertEquals(testRepo + "/foo/bar/1.0.0/bar-1.0.0.jar", checker.getMostRecentArtifactLocation().get().toString());
  }

}
