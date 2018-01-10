package com.github.samcarlberg.updatechecker;

import com.github.zafarkhaja.semver.Version;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class MavenRepo implements Repo {

  private static final String METADATA_XML = "maven-metadata.xml";
  private static final Pattern versionPattern = Pattern.compile("^\\s*<version>(.+)</version>\\s*$");

  @Getter
  private final String name;
  @Getter
  private final URL location;

  private volatile Collection<Version> availableVersions;

  @Override
  public Collection<Version> getAvailableVersions(String group, String artifact) throws IOException {
    URL metadataLocation = generateMetadataLocation(group, artifact);
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(metadataLocation.openStream()))) {
      return availableVersions = reader.lines()
          .map(versionPattern::matcher)
          .filter(Matcher::matches)
          .map(m -> m.group(1))
          .map(Version::valueOf)
          .sorted(Version::compareWithBuildsTo)
          .collect(Collectors.toList());
    }
  }

  private URL generateMetadataLocation(String group, String artifact) throws MalformedURLException {
    return new URL(String.format("%s/%s/%s/%s", location.toString(), group.replace('.', '/'), artifact, METADATA_XML));
  }

  @Override
  public Optional<Version> getMostRecentVersion(String group, String artifact) throws IOException {
    if (availableVersions == null) {
      getAvailableVersions(group, artifact);
    }
    return availableVersions.stream().max(Version.BUILD_AWARE_ORDER);
  }
}
