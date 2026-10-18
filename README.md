[![Ministry of Justice Repository Compliance Badge](https://github-community.service.justice.gov.uk/repository-standards/api/laa-ccms-caab-assessment-api/badge)](https://github-community.service.justice.gov.uk/repository-standards/laa-ccms-caab-assessment-api)

# CCMS CAAB Assessment API

## Logging

The service writes its console logs as [Elastic Common Schema (ECS)](https://www.elastic.co/docs/reference/ecs)
JSON, in line with the LAA monitoring and logging guardrail. This is configured under
`logging.structured` in [`application.yml`](./assessment-service/src/main/resources/application.yml); there is deliberately no
`logback-spring.xml`, as one would override this configuration.

### Running locally

The `local` profile switches structured logging off in favour of a readable console pattern, which
still carries the `[traceId,spanId,sessionID]` of the current request:

```shell
./gradlew :assessment-service:bootRun --args='--spring.profiles.active=local'
```

```text
2026-10-01T23:46:51.422+01:00  INFO 12345 --- [omcat-handler-0] [6abee2db0f6e160ef1468c3af7e9424b,77c38709dbbb96c7,9f5de6e6-b841-4398-932d-d95fb651748c] u.g.l.s.auth.ApiAuthenticationFilter     : Request rejected for endpoint 'GET /assessments': No API access token provided.
```

### Structured (ECS) output

With any other profile, each log event is a single line of JSON. For example (formatted for
readability):

```json
{
  "@timestamp": "2026-10-01T22:46:44.099369Z",
  "log": {
    "level": "INFO",
    "logger": "uk.gov.laa.springboot.auth.ApiAuthenticationFilter"
  },
  "process": {
    "pid": 12345,
    "thread": {
      "name": "tomcat-handler-0"
    }
  },
  "service": {
    "name": "laa-ccms-caab-assessment-api",
    "version": "1.4.2",
    "environment": "dev",
    "node": {
      "name": "laa-ccms-caab-assessment-api-5c8d7f9b4-q7w2e"
    }
  },
  "message": "Request rejected for endpoint 'GET /assessments': No API access token provided.",
  "traceId": "6abee2d4e6452fae071a36da06a8d6b6",
  "spanId": "20cff21cf02075da",
  "sessionID": "9f5de6e6-b841-4398-932d-d95fb651748c",
  "ecs": {
    "version": "8.11"
  }
}
```

| Field                 | Source                                                                                   |
|-----------------------|------------------------------------------------------------------------------------------|
| `service.name`        | `spring.application.name`                                                                |
| `service.version`     | The Gradle project version, substituted for `@version@` in `application.yml` at build time |
| `service.environment` | The active Spring profiles (`SPRING_PROFILES_ACTIVE`), or `default` when none is set     |
| `service.node.name`   | The `HOSTNAME` environment variable, which is the pod name in Kubernetes                 |
| `traceId`, `spanId`   | Micrometer Tracing (Brave), for log events written while a request is being handled      |

### Log levels

| Environment variable   | Logger                | Default |
|------------------------|-----------------------|---------|
| `ROOT_LOGGING_LEVEL`   | root                  | `info`  |
| `SPRING_LOGGING_LEVEL` | `org.springframework` | `info`  |
| `APP_LOGGING_LEVEL`    | `uk.gov.laa.ccms.caab` | `info`  |

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

## Contributing
Follow the [contribution guide](./CONTRIBUTING.md) to make code changes.
### Request correlation and verification

`sessionID` is a generated opaque logging ID, never the authentication session cookie.
Requests with an existing HTTP session reuse its logging ID; stateless requests get a new ID
without creating a session. It is scoped to this service and is not propagated downstream.
`traceId` and `spanId` are supplied by Micrometer while a request is handled; startup logs
have no request correlation. The local pattern includes all three fields.

Run the focused checks with `./gradlew assessment-service:test --tests '*LoggingAcceptanceTest' --tests '*SessionCorrelationFilterTest'`.
They use synthetic requests and write actual captured log lines to `assessment-service/build/logging-evidence/`.
Do not enable request payload or SQL bind parameter logging with real personal data.

Captured synthetic-request screenshots and validation scope: [logging evidence](docs/logging/README.md).
