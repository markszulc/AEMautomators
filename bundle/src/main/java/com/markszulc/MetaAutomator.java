package com.markszulc;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.lang.String;import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Service
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Metadata Automator"),
        @Property(name = Constants.SERVICE_VENDOR, value = "Adobe"),
        @Property(name = "process.label", value = "Metadata Automator")
})
public class MetaAutomator implements WorkflowProcess {

    private static final String TYPE_JCR_PATH = "JCR_PATH";
    private static Logger logger = LoggerFactory.getLogger(MetaAutomator.class);

    public void execute(WorkItem item, WorkflowSession session, MetaDataMap args) throws WorkflowException {
        WorkflowData workflowData = item.getWorkflowData();
        if (workflowData.getPayloadType().equals(TYPE_JCR_PATH)) {
            String path = workflowData.getPayload().toString() + "../../../../jcr:content/metadata";

            //String assetPath = readArgument(args);
            String assetPath = "/content/dam/products/";   // Update metadata only under this path.

            //IF FOOD PATH THEN UPDATE METADATA
            if (path.startsWith(assetPath)) {
                //logger.info("******* FOUND AN ASSET!!!  ** ");

                String assetFilename = workflowData.getPayload().toString();
                String assetSKU = "Error - SKU Not Found";
                String assetBrand = "";
                String assetDescription = "";
                String assetTitle = "";
                String assetBarcode = "";
                String assetLocation = "";
                String assetLanguage = "en";
                String assetOwner = "";
                String assetContributor = "";
                String assetCreator = "";
                String assetExpiry = "";

                Pattern p = Pattern.compile("/([0-9]+)-[0-9]*\\.tif/");
                Matcher m = p.matcher(assetFilename);
                if ( m.find() ) {
                    assetSKU = m.group(1);


                    try {

                        // Assemble node path for metadata "/etc/skumetadata/8/5/3/8534"
                        String nodePath = "/etc/skumetadata/" + assetSKU.substring(0,1) + "/" + assetSKU.substring(1,2) + "/" + assetSKU.substring(2,3) + "/" + assetSKU;
                        Node sourceNode = (Node) session.getSession().getNode(nodePath);


                        assetBrand = sourceNode.getProperty("Brand").getString();
                        assetDescription = sourceNode.getProperty("Description").getString();
                        assetBarcode = sourceNode.getProperty("Barcode").getString();
                        assetLocation = sourceNode.getProperty("Location").getString();
                        assetLanguage = sourceNode.getProperty("Language").getString();
                        assetOwner = sourceNode.getProperty("Copyright Owner").getString();

                    }    catch (RepositoryException e) {
                        throw new WorkflowException(e.getMessage(),e);
                    }


                    assetTitle = assetSKU + " - " + assetBrand + " " + assetDescription;


                    try {
                        Node node = (Node) session.getSession().getItem(path);
                        if (node != null) {
                            node.setProperty("xmpRights:Owner", assetOwner);
                            node.setProperty("Iptc4xmpExt:LocationShown", assetLocation);
                            node.setProperty("dc:language", assetLanguage);
                            node.setProperty("sku", assetSKU);
                            node.setProperty("dc:description", assetDescription);
                            node.setProperty("dc:title", assetTitle);
                            node.setProperty("brand", assetBrand);
                            node.setProperty("barcode", assetBarcode);

                            session.getSession().save();
                            logger.info("**SKU ** " + assetSKU);
                        }
                    } catch (RepositoryException e) {
                        throw new WorkflowException(e.getMessage(),e);
                    }

                }


            }

        }
    }

    private String readArgument(MetaDataMap args) {

        String argument = args.get("PROCESS_ARGS", "false");
        //logger.info("*******ARGS " + argument);
        return argument;

    }

}