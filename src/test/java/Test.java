import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class Test {

    private FhirValidator preconfiguredValidator;

    @BeforeEach
    public void before() {
        FhirContext ctx = FhirContext.forR4();

        // Create a chain that will hold our modules
        ValidationSupportChain supportChain = new ValidationSupportChain();

        // DefaultProfileValidationSupport supplies base FHIR definitions. This is generally required
        // even if you are using custom profiles, since those profiles will derive from the base
        // definitions.
        DefaultProfileValidationSupport defaultSupport = new DefaultProfileValidationSupport(ctx);
        supportChain.addValidationSupport(defaultSupport);

        supportChain.addValidationSupport(new SnapshotGeneratingValidationSupport(ctx));

        // This module supplies several code systems that are commonly used in validation
        supportChain.addValidationSupport(new CommonCodeSystemsTerminologyService(ctx));

        // This module implements terminology services for in-memory code validation
        supportChain.addValidationSupport(new InMemoryTerminologyServerValidationSupport(ctx));

        // Create a PrePopulatedValidationSupport which can be used to load custom definitions.
        // In this example we're loading two things, but in a real scenario we might
        // load many StructureDefinitions, ValueSets, CodeSystems, etc.
        PrePopulatedValidationSupport prePopulatedSupport = new PrePopulatedValidationSupport(ctx);

        IParser parser = ctx.newJsonParser();
            IBaseResource sd1 = parser.parseResource(getClass().getClassLoader().getResourceAsStream("StructureDefinition-PatientForProfileVersionTesting_0.1.0.json"));
            IBaseResource sd2 = parser.parseResource(getClass().getClassLoader().getResourceAsStream("StructureDefinition-PatientForProfileVersionTesting_0.2.0.json"));
            prePopulatedSupport.addStructureDefinition(sd1);
            prePopulatedSupport.addStructureDefinition(sd2);

        // Add the custom definitions to the chain
        supportChain.addValidationSupport(prePopulatedSupport);

        // Wrap the chain in a cache to improve performance
        CachingValidationSupport cache = new CachingValidationSupport(supportChain);

        // Create a validator using the FhirInstanceValidator module. We can use this
        // validator to perform validation
        FhirInstanceValidator validatorModule = new FhirInstanceValidator(cache);
        preconfiguredValidator = ctx.newValidator().registerValidatorModule(validatorModule);
    }

    @org.junit.jupiter.api.Test
    public void testValidateResourceWithProfileAndVersion020AgainstAvailableProfile() {
        Patient input = new Patient();
        input.setMeta(new Meta().addProfile("http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.2.0"));

        ValidationResult result = preconfiguredValidator.validateWithResult(input);

        assertFalse(result.isSuccessful());
        assertEquals(2, result.getMessages().size());
        assertEquals("Patient.active: minimum required = 1, but only found 0 (from http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.2.0)", result.getMessages().get(0).getMessage());
        assertEquals("Patient.gender: minimum required = 1, but only found 0 (from http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.2.0)", result.getMessages().get(1).getMessage());
    }

    @org.junit.jupiter.api.Test
    public void testValidateResourceWithProfileAndVersion010AgainstAvailableProfile() {
        Patient input = new Patient();
        input.setMeta(new Meta().addProfile("http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.1.0"));

        ValidationResult result = preconfiguredValidator.validateWithResult(input);

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getMessages().size());
        assertEquals("Patient.gender: minimum required = 1, but only found 0 (from http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.1.0)", result.getMessages().get(0).getMessage());
    }

    @org.junit.jupiter.api.Test
    public void testValidateResourceWithProfileAndVersionAgainstUnavailableProfile() {
        Patient input = new Patient();
        input.setMeta(new Meta().addProfile("http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.3.0"));

        ValidationResult result = preconfiguredValidator.validateWithResult(input);

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getMessages().size());
        assertEquals("Profile reference 'http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.3.0' has not been checked because it is unknown", result.getMessages().get(0).getMessage());
    }


    @org.junit.jupiter.api.Test
    public void testValidateResourceWithProfileAndNoVersionAgainstMultipleAvailableProfiles() {
        Patient input = new Patient();
        input.setMeta(new Meta().addProfile("http://example.org/StructureDefinition/PatientForProfileVersionTesting"));

        ValidationResult result = preconfiguredValidator.validateWithResult(input);

        assertFalse(result.isSuccessful());
        assertEquals(2, result.getMessages().size());
        // it seems that the newest available profile is chosen
        assertEquals("Patient.active: minimum required = 1, but only found 0 (from http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.2.0)", result.getMessages().get(0).getMessage());
        assertEquals("Patient.gender: minimum required = 1, but only found 0 (from http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.2.0)", result.getMessages().get(1).getMessage());
    }

    @org.junit.jupiter.api.Test
    public void testValidateResourceWithoutProfileAndVersionAgainstAvailableProfileExplicitly() {
        Patient input = new Patient();
        ValidationResult result = preconfiguredValidator.validateWithResult(input, new ValidationOptions().addProfile("http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.2.0"));

        assertFalse(result.isSuccessful());
        assertEquals(2, result.getMessages().size());
        assertEquals("Patient.active: minimum required = 1, but only found 0 (from http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.2.0)", result.getMessages().get(0).getMessage());
        assertEquals("Patient.gender: minimum required = 1, but only found 0 (from http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.2.0)", result.getMessages().get(1).getMessage());
    }


    @org.junit.jupiter.api.Test
    public void testValidateResourceWithoutProfileAndVersion010AgainstAvailableProfileExplicitly() {
        Patient input = new Patient();
        ValidationResult result = preconfiguredValidator.validateWithResult(input, new ValidationOptions().addProfile("http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.1.0"));

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getMessages().size());
        assertEquals("Patient.gender: minimum required = 1, but only found 0 (from http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.1.0)", result.getMessages().get(0).getMessage());
    }

    @org.junit.jupiter.api.Test
    public void testValidateResourceWithoutProfileAndVersionAgainstUnavailableProfileExplicitly() {
        Patient input = new Patient();

        ValidationResult result = preconfiguredValidator.validateWithResult(input, new ValidationOptions().addProfile("http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.3.0"));

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getMessages().size());
        assertEquals("Profile reference 'http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.3.0' has not been checked because it is unknown", result.getMessages().get(0).getMessage());
    }

    @org.junit.jupiter.api.Test
    public void testValidateResourceWithProfileAndVersionAgainstUnavailableProfileExplicitly() {
        Patient input = new Patient();
        input.setMeta(new Meta().addProfile("http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.3.0"));
        ValidationResult result = preconfiguredValidator.validateWithResult(input, new ValidationOptions().addProfile("http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.3.0"));

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getMessages().size());
        assertEquals("Profile reference 'http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.3.0' has not been checked because it is unknown", result.getMessages().get(0).getMessage());
    }

    @org.junit.jupiter.api.Test
    public void testValidateResourceWithoutProfileAndNoVersionAgainstMultipleAvailableProfilesExplicitly() {
        Patient input = new Patient();
        input.setMeta(new Meta().addProfile("http://example.org/StructureDefinition/PatientForProfileVersionTesting"));

        ValidationResult result = preconfiguredValidator.validateWithResult(input, new ValidationOptions().addProfile("http://example.org/StructureDefinition/PatientForProfileVersionTesting"));

        assertFalse(result.isSuccessful());
        assertEquals(2, result.getMessages().size());
        // it seems that the newest available profile is chosen
        assertEquals("Patient.active: minimum required = 1, but only found 0 (from http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.2.0)", result.getMessages().get(0).getMessage());
        assertEquals("Patient.gender: minimum required = 1, but only found 0 (from http://example.org/StructureDefinition/PatientForProfileVersionTesting|0.2.0)", result.getMessages().get(1).getMessage());
    }

//    // Do we have any errors or fatal errors?
//        System.out.println(result.isSuccessful());
//
//    // Show the issues
//        for (SingleValidationMessage next : result.getMessages()) {
//        System.out.println(" Next issue " + next.getSeverity() + " - " + next.getLocationString() + " - " + next.getMessage());
//    }
}
