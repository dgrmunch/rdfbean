package com.mysema.rdfbean.object;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.mysema.rdfbean.domains.*;

public class DomainExporterTest {

    private final DefaultConfiguration configuration = new DefaultConfiguration();

    @Test
    public void CompanyDepartmentEmployeeDomain() throws IOException{
        configuration.addClasses(
                CompanyDepartmentEmployeeDomain.Company.class,
                CompanyDepartmentEmployeeDomain.Department.class,
                CompanyDepartmentEmployeeDomain.Employee.class);

        DomainExporter exporter = new DomainExporter(new File("target/CompanyDepartmentEmployeeDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void ContextDomain() throws IOException{
        configuration.addClasses(
                ContextDomain.Entity1.class,
                ContextDomain.Entity1.class,
                ContextDomain.Entity1.class);

        DomainExporter exporter = new DomainExporter(new File("target/ContextDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void DateTimeDomain() throws IOException{
        configuration.addClasses(DateTimeDomain.Literals.class);

        DomainExporter exporter = new DomainExporter(new File("target/DateTimeDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void EntityDocumentRevisionDomain() throws IOException{
        configuration.addClasses(EntityDocumentRevisionDomain.Document.class, EntityDocumentRevisionDomain.Revision.class);

        DomainExporter exporter = new DomainExporter(new File("target/EntityDocumentRevisionDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void EntityDomain() throws IOException{
        configuration.addClasses(EntityDomain.Entity.class);

        DomainExporter exporter = new DomainExporter(new File("target/EntityDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void EntityRevisionTermDomain() throws IOException{
        configuration.addClasses(
                EntityRevisionTermDomain.Entity.class,
                EntityRevisionTermDomain.EntityRevision.class,
                EntityRevisionTermDomain.Term.class);

        DomainExporter exporter = new DomainExporter(new File("target/EntityRevisionTermDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void IdentityDomain() throws IOException{
        configuration.addClasses(IdentityDomain.Entity1.class, IdentityDomain.Entity2.class);

        DomainExporter exporter = new DomainExporter(new File("target/IdentityDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void InferenceDomain() throws IOException{
        configuration.addClasses(InferenceDomain.Entity1.class, InferenceDomain.Entity2.class, InferenceDomain.Entity3.class);

        DomainExporter exporter = new DomainExporter(new File("target/InferenceDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void ItemDomain() throws IOException{
        configuration.addClasses(ItemDomain.Item.class);

        DomainExporter exporter = new DomainExporter(new File("target/ItemDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void ListDomain() throws IOException{
        configuration.addClasses(ListDomain.Element.class, ListDomain.Elements.class, ListDomain.Identifiable.class,
                ListDomain.LinkElement.class, ListDomain.TextElement.class);

        DomainExporter exporter = new DomainExporter(new File("target/ListDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void LiteralsDomain() throws IOException{
        configuration.addClasses(LiteralsDomain.Literals.class);

        DomainExporter exporter = new DomainExporter(new File("target/LiteralsDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void LoadDomain() throws IOException{
        configuration.addClasses(LoadDomain.Document.class, LoadDomain.Entity.class, LoadDomain.Revision.class);

        DomainExporter exporter = new DomainExporter(new File("target/LoadDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void NoteRevisionTermDomain() throws IOException{
        configuration.addClasses(NoteRevisionTermDomain.Note.class, NoteRevisionTermDomain.Term.class);

        DomainExporter exporter = new DomainExporter(new File("target/NoteRevisionTermDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void NoteTermDomain() throws IOException{
        configuration.addClasses(NoteTermDomain.Note.class, NoteTermDomain.Term.class);

        DomainExporter exporter = new DomainExporter(new File("target/NoteTermDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void NoteTypeDomain() throws IOException{
        configuration.addClasses(NoteTypeDomain.Note.class, NoteTypeDomain.NoteType.class);

        DomainExporter exporter = new DomainExporter(new File("target/NoteTypeDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void PropertiesDomain() throws IOException{
        configuration.addClasses(PropertiesDomain.Person.class, PropertiesDomain.Iteration.class, PropertiesDomain.Project.class);

        DomainExporter exporter = new DomainExporter(new File("target/PropertiesDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void ResourceDomain() throws IOException{
        configuration.addClasses(ResourceDomain.Resource.class);

        DomainExporter exporter = new DomainExporter(new File("target/ResourceDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void SimpleDomain() throws IOException{
        configuration.addClasses(SimpleDomain.SimpleType.class, SimpleDomain.SimpleType2.class);

        DomainExporter exporter = new DomainExporter(new File("target/SimpleDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void UserDepartmentCompanyDomain() throws IOException{
        configuration.addClasses(UserDepartmentCompanyDomain.Company.class,
                UserDepartmentCompanyDomain.Department.class, UserDepartmentCompanyDomain.User.class);

        DomainExporter exporter = new DomainExporter(new File("target/UserDepartmentCompanyDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void UserDomain() throws IOException{
        configuration.addClasses(UserDomain.User.class);

        DomainExporter exporter = new DomainExporter(new File("target/UserDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void UserProfileDomain() throws IOException{
        configuration.addClasses(UserProfileDomain.Identifiable.class, UserProfileDomain.User.class, UserProfileDomain.Profile.class);

        DomainExporter exporter = new DomainExporter(new File("target/UserProfileDomain"), configuration);
        exporter.execute();
    }

    @Test
    public void UserProjectionDomain() throws IOException{
        configuration.addClasses(UserProjectionDomain.User.class);

        DomainExporter exporter = new DomainExporter(new File("target/UserProjectionDomain"), configuration);
        exporter.execute();
    }

}
