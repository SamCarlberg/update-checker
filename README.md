# update-checker

A simple Java library that checks for updates to software in Maven repositories.
It provides means for getting the most recent versions of a software package from arbitrary
Maven repositories.

## Basic use

```java
String group = "...";
String artifactName = "...";
String currentVersion = "...";

UpdateChecker updateChecker = new UpdateChecker(group, artifactName, currentVersion);
updateChecker.useMavenRepos(repo1, repo2, ...);

if (updateChecker.getStatus() == UpdateStatus.OUTDATED) {
  updateChecker.getMostRecentArtifact().ifPresent(artifact -> {
    System.out.println("The most recent artifact is version " + artifact.getVersion() 
        + " on Maven repo " + artifact.getMavenRepo());
  });
  updateChecker.getMostRecentArtifactLocation().ifPresent(url -> {
    System.out.println("The most recent artifact can be downloaded at " + url);
  });
}
```
