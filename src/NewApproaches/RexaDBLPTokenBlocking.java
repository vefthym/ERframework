package NewApproaches;

import static BlockBuilding.IBlockBuilding.DOC_ID;
import static BlockBuilding.IBlockBuilding.VALUE_LABEL;
import BlockBuilding.StandardBlocking;
import DataModel.Attribute;
import DataModel.EntityProfile;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;

/**
 *
 * @author G.A.P. II
 */
public class RexaDBLPTokenBlocking extends StandardBlocking {

    private static final Logger LOGGER = Logger.getLogger(RexaDBLPTokenBlocking.class.getName());

    protected Set<String> labelPredicates;    

    public RexaDBLPTokenBlocking() {
        labelPredicates = new HashSet<>();                
        labelPredicates.add("http://xmlns.com/foaf/0.1/name");
        labelPredicates.add("http://www.w3.org/2000/01/rdf-schema#label");        
    }

    @Override
    protected void buildBlocks() {
        setMemoryDirectory();

        IndexWriter iWriter1 = openWriter(indexDirectoryD1);
        indexEntities(iWriter1, entityProfilesD1);
        closeWriter(iWriter1);

        IndexWriter iWriter2 = openWriter(indexDirectoryD2);
        indexEntities(iWriter2, entityProfilesD2);
        closeWriter(iWriter2);
    }

    @Override
    protected void indexEntities(IndexWriter index, List<EntityProfile> entities) {
        try {
            int counter = 0;
            for (EntityProfile profile : entities) {
                Document doc = new Document();
                doc.add(new StoredField(DOC_ID, counter++));
                for (Attribute attribute : profile.getAttributes()) {
                    getBlockingKeys(attribute.getValue()).stream().filter((key) -> (0 < key.trim().length())).forEach((key) -> {
                        doc.add(new StringField(VALUE_LABEL, key.trim(), Field.Store.YES));
                    });                    
                    //exact match on labels
                    /*if (labelPredicates.contains(attribute.getName())) {
                        doc.add(new StringField(VALUE_LABEL, attribute.getValue().toLowerCase().replaceAll("[^a-z0-9 ]", "").trim() + "_LP", Field.Store.YES));                        
                    }*/
                    //token blocking on labels                    
                    if (labelPredicates.contains(attribute.getName())) {
                        getBlockingKeys(attribute.getValue()).stream().filter((key) -> (0 < key.trim().length())).forEach((key) -> {
                            doc.add(new StringField(VALUE_LABEL, key.trim() + "_LP", Field.Store.YES));
                        });
                    }
                    
                }                 
                index.addDocument(doc);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
}
