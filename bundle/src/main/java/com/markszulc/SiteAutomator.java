package com.markszulc;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

@Component
@Service
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Site Automator"),
        @Property(name = Constants.SERVICE_VENDOR, value = "Adobe"),
        @Property(name = "process.label", value = "Site Automator")
})
public class SiteAutomator implements WorkflowProcess {

    private static final String TYPE_JCR_PATH = "JCR_PATH";
    private static Logger logger = LoggerFactory.getLogger(SiteAutomator.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    public void execute(WorkItem item, WorkflowSession session, MetaDataMap args) throws WorkflowException {
        WorkflowData workflowData = item.getWorkflowData();
        logger.info("******* BUILDING THE SITE!!!  ** ");
        if (workflowData.getPayloadType().equals(TYPE_JCR_PATH)) {
            String path = workflowData.getPayload().toString();
            //logger.info("********* " + path);

            try {
                Node sourceNode = (Node) session.getSession().getNode(path);
                String siteCompany = sourceNode.getProperty("company").getString();
                String siteDescription = sourceNode.getProperty("description").getString();
                String siteDomain = sourceNode.getProperty("domain").getString();

                logger.info("********* Site: " + siteCompany);

                ResourceResolver resourceResolver = null;
                try {
                    resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
                }
                catch (LoginException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

                PageManager pm = resourceResolver.adaptTo(PageManager.class);


                //base page - this would be dynamic
                Page basePage = pm.getContainingPage("/content/geometrixx/en");

                String newPagePath = "/content/" + siteDomain;


                try{
                    Page newPage = pm.copy(basePage, newPagePath, null, true, true);
                    Node myNode = (Node) session.getSession().getItem(newPagePath);
                    Node myNodeContent = myNode.getNode("jcr:content");

                    logger.info("********* WORKING ON NODE" + myNodeContent.getPath());
                    myNodeContent.setProperty("jcr:title", siteCompany);
                    myNodeContent.setProperty("jcr:description", siteDescription);

                    myNodeContent.setProperty("cq:designPath", "/etc/designs/geometrixx");
                    myNodeContent.getNode("lead").setProperty("jcr:title", "Welcome to " + siteCompany);
                    myNodeContent.getNode("lead").setProperty("jcr:description", siteDescription);


                    session.getSession().save();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (RepositoryException e) {
                throw new WorkflowException(e.getMessage(),e);
            }
        }

    }
}
