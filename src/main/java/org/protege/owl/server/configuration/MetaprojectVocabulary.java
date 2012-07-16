package org.protege.owl.server.configuration;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLObjectProperty;

public class MetaprojectVocabulary {
	public static final String NS = "http://protege.stanford.edu/ontologies/metaproject.owl";
	
	public static final OWLClass STANDARD_SERVER;
	public static final OWLClass RMI_TRANSPORT;
	
	public static final OWLObjectProperty HAS_SERVER_FILTER;
	public static final OWLObjectProperty HAS_TRANSPORT;
	
	public static final OWLDataProperty HAS_ROOT_PATH;
	public static final OWLDataProperty HAS_HOST_NAME;
	public static final OWLDataProperty HAS_HOST_PORT;
	
	
	static {
		OWLDataFactory factory = OWLManager.getOWLDataFactory();
		STANDARD_SERVER = factory.getOWLClass(IRI.create(NS + "#StandardOWL2Server"));
		RMI_TRANSPORT   = factory.getOWLClass(IRI.create(NS + "#RMI"));
		
		HAS_SERVER_FILTER = factory.getOWLObjectProperty(IRI.create(NS + "#hasServerFilter"));
		HAS_TRANSPORT     = factory.getOWLObjectProperty(IRI.create(NS + "#hasTransport"));
		
		HAS_ROOT_PATH     = factory.getOWLDataProperty(IRI.create(NS + "#hasServerRootPath"));
		HAS_HOST_NAME     = factory.getOWLDataProperty(IRI.create(NS + "#hasHostName"));
		HAS_HOST_PORT     = factory.getOWLDataProperty(IRI.create(NS + "#hasHostPort"));
	}

	
	
	private MetaprojectVocabulary() {
		;
	}
}
