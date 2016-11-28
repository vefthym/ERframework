package DataReader;

import DataModel.Attribute;
import DataModel.EntityProfile;
import DataReader.EntityReader.EntityRDFReader;
import java.util.List;

/**
 *
 * @author G.A.P. II
 */

public class TestRdfReader {
    public static void main(String[] args) {
        String filePath = "C:\\Users\\VASILIS\\Documents\\Datasets_papadaki\\person1\\person12.nt";
        EntityRDFReader n3reader = new EntityRDFReader(filePath);
//        n3reader.setAttributesToExclude(new String[]{"http://www.w3.org/2000/01/rdf-schema#label", "http://www.w3.org/2000/01/rdf-schema#label"});
        List<EntityProfile> profiles = n3reader.getEntityProfiles();
        System.out.println("Found "+ profiles.size()+ " profiles");
        /*for (EntityProfile profile : profiles) {
            System.out.println("\n\n" + profile.getEntityUrl());
            for (Attribute attribute : profile.getAttributes()) {
                System.out.print(attribute.toString());
                System.out.println();
            }
        }*/
        n3reader.storeSerializedObject(profiles, "C:\\Users\\VASILIS\\Documents\\Datasets_papadaki\\person1\\person12Profiles");
    }
}