# laa-ccms-caab-assessment-api

## Common Components

This API uses components from the [LAA CCMS Common Library](https://github.com/ministryofjustice/laa-ccms-spring-boot-common):

- [laa-ccms-spring-boot-plugin](https://github.com/ministryofjustice/laa-ccms-spring-boot-common?tab=readme-ov-file#laa-ccms-spring-boot-gradle-plugin-for-java--spring-boot-projects)
- [laa-ccms-spring-boot-starter-auth](https://github.com/ministryofjustice/laa-ccms-spring-boot-common/tree/main/laa-ccms-spring-boot-starters/laa-ccms-spring-boot-starter-auth)


## Deploying features

Feature branches be used to create deployments to `development` or `test` environments in Cloud Platform (Amazon EKS), via our Helm Chart repository.

The feature branches must be in the form:

- `feature-dev/<short name / ticket number>` - deploy to `development`
- `feature-test/<short name / ticket number>` - deploy to `test`

These branches trigger the Deploy feature pipeline which will publish an image and update the helm chart that corresponds to this service with the published image version. This will then trigger a deployment. See [Feature deployments](https://github.com/ministryofjustice/laa-ccms-caab-helm-charts?tab=readme-ov-file#feature-deployments) in the Helm chart repository for more details.

## Snyk code analysis (CI/CD)
This project publishes vulnerability scans to the [LAA Snyk Dashboard (Google SSO)](https://app.snyk.io/org/legal-aid-agency).

If you cannot see the LAA organisation when logged into the dashboard,
please ask your lead developer/architect to have you added.

Scans will be triggered in two ways:

- Main branch - on commit, a vulnerability scan will be run and published to both the Snyk
  server and GitHub Code Scanning. Vulnerabilites will not fail the build.
- Feature branches - on commit, a vulnerability scan will be run to identify any new
  vulnerabilites (compared to the main branch). If new vulnerabilites have been raised. A code
  scan will also run to identify known security issues within the source code. If any issues are
  found, the build will fail.

### Running Snyk locally
To run Snyk locally, you will need to [install the Snyk CLI](https://docs.snyk.io/snyk-cli/install-or-update-the-snyk-cli).

Once installed, you will be able to run the following commands:

```shell
snyk test
```
For open-source vulnerabilies and licence issues. See [`snyk test`](https://docs.snyk.io/snyk-cli/commands/test).

```shell
snyk code test
```
For Static Application Security Testing (SAST) - known security issues. See [`snyk code test`](https://docs.snyk.io/snyk-cli/commands/code-test).

A [JetBrains Plugin](https://plugins.jetbrains.com/plugin/10972-snyk-security) is also available to integrate with your IDE. In addition to
vulnerabilities, this plugin will also report code quality issues.

### Configuration (`.snyk`)

The [.snyk](.snyk) file is used to configure exclusions for scanning. If a vulnerability is not
deemed to be a threat, or will be dealt with later, it can be added here to stop the pipeline
failing. See [documentation](https://docs.snyk.io/manage-risk/policies/the-.snyk-file) for more details.

### False Positives

Snyk may report that new vulnerabilities have been introduced on a feature branch and fail the
pipeline, even if this is not the case. As newly identified vulnerabilities are always being
published, the report for the main branch may become outdated when a new vulnerability is published.

If you think this may be the case, simply re-run the `monitor` command against the `main` branch
to update the report on the Snyk server, then re-run your pipeline.

Please ensure this matches the command used by the [pr-merge-main](.github/workflows/build-main.yml)
workflow to maintain consistency.

```shell
snyk monitor --org=legal-aid-agency --all-projects --exclude=build,generated --target-reference=main
```

You should then see the new vulnerability in the LAA Dashboard, otherwise it is a new
vulnerability introduced on the feature branch that needs to be resolved.
