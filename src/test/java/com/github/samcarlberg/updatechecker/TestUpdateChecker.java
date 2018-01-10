package com.github.samcarlberg.updatechecker;

import com.github.zafarkhaja.semver.Version;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUpdateChecker {

  private static Repo testRepo;

  @BeforeAll
  public static void setupRepo() throws MalformedURLException {
    testRepo = Repo.maven(
        "Test",
        new URL(TestUpdateChecker.class.getResource("/").toExternalForm().replace("classes", "resources"))
    );
  }

  @Test
  public void testOutdated() throws IOException {
    UpdateChecker checker = UpdateChecker.create("foo", "bar", "0.4.0", testRepo);
    assertEquals(UpdateStatus.OUTDATED, checker.getStatus(), "Unexpected status");
    assertEquals(Version.valueOf("1.0.0"), checker.getMostRecentVersion().get());
    assertEquals(testRepo.getLocation() + "foo/bar/1.0.0/bar-1.0.0.jar", checker.getMostRecentArtifactLocation().get().toString());
  }

  @Test
  public void testUpToDate() {
    UpdateChecker checker = UpdateChecker.create("foo", "bar", "1.0.0", testRepo);
    assertEquals(UpdateStatus.UP_TO_DATE, checker.getStatus());
  }

  @Test
  public void testLocalVersionNewer() {
    UpdateChecker checker = UpdateChecker.create("foo", "bar", "9.9.9", testRepo);
    assertEquals(UpdateStatus.UP_TO_DATE, checker.getStatus());
  }

  @Test
  public void testNoRepos() {
    UpdateChecker checker = new UpdateChecker("foo", "bar", "1.0.0");
    assertEquals(UpdateStatus.UNKNOWN, checker.getStatus());
  }

  @Test
  public void testNonPresentArtifact() {
    UpdateChecker checker = UpdateChecker.create("not.a.known.group", "not-a-known-artifact", "0.0.0", testRepo);
    assertEquals(UpdateStatus.UNKNOWN, checker.getStatus());
    assertTrue(checker.getVersionsSafe().isEmpty());
  }

}
