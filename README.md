A simple example to demonstrate that the HAPI FHIR Validator does not respect profiles for validation that were supplied using the `ValidationOptions` object.

The tests can be run with this command:

```
mvn test
```

All tests will succeed expect for the test named `testValidateResourceWithoutProfileAndVersionAgainstUnavailableProfileExplicitly`:

```
[INFO] Results:
[INFO] 
[ERROR] Failures: 
[ERROR]   ValidationTest.testValidateResourceWithoutProfileAndVersionAgainstUnavailableProfileExplicitly:146 expected: <false> but was: <true>
[INFO] 
[ERROR] Tests run: 9, Failures: 1, Errors: 0, Skipped: 0
```

The code in this project calls the validator as is described in the documentation [here](https://hapifhir.io/hapi-fhir/docs/validation/instance_validator.html#running-the-validator). This documentation right now claims the following statement, that is falsified by this example project.

```java
/*
 * Note: You can also explicitly declare a profile to validate against
 * using the block below.
 */
// ValidationResult result = validator.validateWithResult(obs, new ValidationOptions().addProfile("http://myprofile.com"));
```
