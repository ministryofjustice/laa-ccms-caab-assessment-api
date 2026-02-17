[![Ministry of Justice Repository Compliance Badge](https://github-community.service.justice.gov.uk/repository-standards/api/laa-ccms-caab-assessment-api/badge)](https://github-community.service.justice.gov.uk/repository-standards/laa-ccms-caab-assessment-api)

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

## Contributing
Follow the [contribution guide](./CONTRIBUTING.md) to make code changes.