package org.protege.owl.server.changes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.protege.owl.server.api.ChangeHistory;
import org.protege.owl.server.api.ChangeMetaData;
import org.protege.owl.server.api.DocumentFactory;
import org.protege.owl.server.api.OntologyDocumentRevision;
import org.protege.owl.server.api.SingletonChangeHistory;
import org.protege.owl.server.api.client.RemoteOntologyDocument;
import org.protege.owl.server.api.client.VersionedOntologyDocument;
import org.protege.owl.server.changes.format.OWLInputStream;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

public class DocumentFactoryImpl implements DocumentFactory, Serializable {
    private static final long serialVersionUID = -4952738108103836430L;
    public static Logger logger = Logger.getLogger(DocumentFactoryImpl.class.getCanonicalName());
    private transient OWLObjectRenderer renderer;
	
    @Override
    public ChangeHistory createEmptyChangeDocument(OntologyDocumentRevision revision) {
        return new ChangeHistoryImpl(this, revision, null, null);
    }
    
	@Override
	public SingletonChangeHistory createChangeDocument(List<OWLOntologyChange> changes,
											   ChangeMetaData metaData, 
											   OntologyDocumentRevision start) {
		return new SingletonChangeHistoryImpl(this, start, changes, metaData);
	}
	
	@Override
	public VersionedOntologyDocument createVersionedOntology(OWLOntology ontology,
													    RemoteOntologyDocument serverDocument,
													    OntologyDocumentRevision revision) {
		ChangeHistory localChanges = createEmptyChangeDocument(OntologyDocumentRevision.START_REVISION);
		return new VersionedOntologyDocumentImpl(ontology, serverDocument, revision, localChanges);
	}
	
	@Override
	public boolean hasServerMetadata(OWLOntology ontology) {
		File ontologyFile = VersionedOntologyDocumentImpl.getBackingStore(ontology);
		return ontologyFile != null && VersionedOntologyDocumentImpl.getMetaDataFile(ontologyFile).exists();
	}
	
	@Override
	public boolean hasServerMetadata(IRI ontologyDocumentLocation) {
	    if (!ontologyDocumentLocation.getScheme().equals("file")) {
	        return false;
	    }
	    File ontologyFile = new File(ontologyDocumentLocation.toURI());
	    return VersionedOntologyDocumentImpl.getMetaDataFile(ontologyFile).exists();
	}
	
	@Override
	public IRI getServerLocation(OWLOntology ontology) throws IOException {
        File ontologyFile = VersionedOntologyDocumentImpl.getBackingStore(ontology);
        return getServerLocation(ontologyFile);
	}
	
	@Override
	public IRI getServerLocation(IRI ontologyDocumentLocation) throws FileNotFoundException, IOException {
	    File ontologyFile = new File(ontologyDocumentLocation.toURI());
	    return getServerLocation(ontologyFile);
	}
	
	private IRI getServerLocation(File ontologyFile) throws FileNotFoundException, IOException {
        ObjectInputStream ois = null;
        try {
            File historyFile = VersionedOntologyDocumentImpl.getMetaDataFile(ontologyFile);
            ois = new ObjectInputStream(new FileInputStream(historyFile));
            RemoteOntologyDocument serverDocument = (RemoteOntologyDocument) ois.readObject();
            return serverDocument.getServerLocation();
        }
        catch (ClassNotFoundException cnfe) {
            throw new IOException("Class Loader issues when hydrating ontology history document", cnfe);
        }
        finally {
            if (ois != null) {
                ois.close();
            }
        }	    
	}

	@Override
	public VersionedOntologyDocument getVersionedOntologyDocument(OWLOntology ontology) throws IOException {
	    ObjectInputStream ois = null;
	    try {
	        File ontologyFile = VersionedOntologyDocumentImpl.getBackingStore(ontology);
	        File metaDataFile = VersionedOntologyDocumentImpl.getMetaDataFile(ontologyFile);
	        File historyFile  = VersionedOntologyDocumentImpl.getHistoryFile(ontologyFile);
	        
	        ois = new ObjectInputStream(new FileInputStream(metaDataFile));
	        
	        RemoteOntologyDocument serverDocument = (RemoteOntologyDocument) ois.readObject();
	        OntologyDocumentRevision revision = (OntologyDocumentRevision) ois.readObject();
	        ChangeHistory localChanges;
	        if (historyFile.exists()) {
	            localChanges = new BackgroundLoadChangeHistory(this, historyFile);
	        }
	        else {
	            localChanges = createEmptyChangeDocument(OntologyDocumentRevision.START_REVISION);
	        }
	        return new VersionedOntologyDocumentImpl(ontology, serverDocument, revision, localChanges);
	    }
	    catch (ClassNotFoundException cnfe) {
	        throw new IOException("Class Loader issues when hydrating ontology history document", cnfe);
	    }
	    finally {
	        if (ois != null) {
	            ois.close();
	        }
	    }
	}
	
	@Override
	public ChangeHistory readChangeDocument(InputStream in,
											 OntologyDocumentRevision start, OntologyDocumentRevision end) throws IOException {
        ObjectInputStream ois;
        long startTime = System.currentTimeMillis();
	    try {
			if (in instanceof ObjectInputStream) {
				ois = (ObjectInputStream) in;
			}
			else {
				ois = new ObjectInputStream(in);
			}
			return readChangeDocument(ois, start, end);
		}
		catch (IOException ioe) {
			logger.log(Level.WARNING, "Exception caught deserializing change document", ioe);
			throw ioe;
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Exception caught deserializing change document", e);
			throw new IOException(e);
		}
		catch (Error e) {
			logger.log(Level.WARNING, "Exception caught deserializing change document", e);
			throw e;
		}
	    finally {
	        logLongRead(System.currentTimeMillis() - startTime);
	    }
	}
	
	@SuppressWarnings("deprecation")
	private ChangeHistory readChangeDocument(ObjectInputStream ois,
	                                         OntologyDocumentRevision start, OntologyDocumentRevision end) throws IOException, ClassNotFoundException  {
	    OntologyDocumentRevision startRevision = (OntologyDocumentRevision) ois.readObject();
	    if (start == null) {
	        start = startRevision;
	    }
	    @SuppressWarnings("unchecked")
	    SortedMap<OntologyDocumentRevision, ChangeMetaData> metaData = (SortedMap<OntologyDocumentRevision, ChangeMetaData>) ois.readObject();
	    List<List<OWLOntologyChange>> changes = new ArrayList<List<OWLOntologyChange>>();
	    OWLInputStream owlStream = new OWLInputStream(ois);
	    int count = ois.readInt();
	    OntologyDocumentRevision revision = startRevision;
	    for (int i = 0; i < count; i++,revision = revision.next()) {
	        @SuppressWarnings("unchecked")
	        List<OWLOntologyChange> changeList = (List<OWLOntologyChange>) owlStream.read();
	        if (revision.compareTo(start) >= 0 && (end == null || revision.compareTo(end) < 0)) {
	            changes.add(changeList);
	        }
	    }
	    if (end == null) {
	        end = start.add(changes.size());
	    }
	    if (!metaData.isEmpty()) {
	        metaData = metaData.tailMap(start).headMap(end);
	    }
	    return new ChangeHistoryImpl(start, this, changes, metaData); 
	}
	
	private void logLongRead(long interval) {
	    if (interval > 1000) {
	        logger.info("Read of change list took " + (interval/1000) + " seconds.");
	    }
	}

	@Override
	public OWLObjectRenderer getOWLRenderer() {
	    if (renderer == null) {
	        renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
	    }
	    return renderer;
	}

}
