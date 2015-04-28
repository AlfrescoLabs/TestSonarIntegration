package org.alfresco.test.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.commons.lang3.StringUtils;

/**
 * Create documents and folders using CMIS.
 * 
 * @author Bocancea Bogdan
 */

public class ContentService extends CMISUtil
{
    public ContentService(AlfrescoHttpClientFactory alfrescoHttpClientFactory)
    {
        super(alfrescoHttpClientFactory);
    }
    
    /**
     * Create a new folder
     * 
     * @param userName
     * @param password
     * @return Cmis Session
     * @throws Exception if error
     */
    public Folder createFolder(final String userName,
                               final String password,
                               final String folderName,
                               final String siteName) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(folderName)
                || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }      
        Map<String, String> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
        properties.put(PropertyIds.NAME, folderName);
        Session session = getCMISSession(userName, password);
        try
        {
            Folder documentLibrary = (Folder) session.getObjectByPath("/Sites/" + siteName + "/documentLibrary");
            Folder newFolder = documentLibrary.createFolder(properties);
            return newFolder;
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Invalid Site " + siteName, nf);
        }  
        catch(CmisContentAlreadyExistsException ae)
        {
            throw new CmisRuntimeException("Folder already exists " + folderName, ae);
        } 
    }
    
    /**
     * Delete a folder
     * 
     * @param userName
     * @param password
     * @param folderName
     * @return true if folder is deleted
     * @throws Exception if error
     */
    public void deleteFolder(final String userName,
                             final String password,
                             final String siteName,
                             final String folderName) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(folderName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        try
        {
            Session session = getCMISSession(userName, password); 
            String folderId = getNodeRef(userName, password, siteName, folderName);
            session.getObject(folderId).delete();         
        }
        catch(CmisInvalidArgumentException nf)
        {
            throw new CmisRuntimeException("Invalid folder " + folderName, nf);
        }    
        catch(CmisConstraintException ce)
        {
            throw new CmisRuntimeException("Cannot delete folder with at least one child", ce);
        }  
    }
    
    /**
     * Create a new document using CMIS
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param DocumentType 
     * @param docName
     * @param docContent
     * @return document
     * @throws Exception if error
     */
    public Document createDocument(final String userName,
                                   final String password,
                                   final String siteName,
                                   final DocumentType fileType,
                                   final String docName,
                                   final String docContent) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(docName)
                || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        properties.put(PropertyIds.NAME, docName);     
        Session session = getCMISSession(userName, password);
        try
        {
            byte[] content = docContent.getBytes();
            InputStream stream = new ByteArrayInputStream(content);
            ContentStream contentStream = session.getObjectFactory().createContentStream(docName, Long.valueOf(content.length), fileType.type, stream);
            Folder documentLibrary = (Folder) session.getObjectByPath("/Sites/" + siteName + "/documentLibrary");
            Document d = documentLibrary.createDocument(properties, contentStream, VersioningState.MAJOR);
            return d;
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Invalid Site " + siteName, nf);
        }
        catch(CmisContentAlreadyExistsException ae)
        {
            throw new CmisRuntimeException("Document already exits " + siteName, ae);
        }
    }
    
    /**
     * Create a new document into a folder using CMIS
     * 
     * @param userName
     * @param password
     * @param siteName
     * @param folderName
     * @param DocumentType 
     * @param docName
     * @param docContent
     * @return document
     * @throws Exception if error
     */
    public Document createDocumentInFolder(final String userName,
                                           final String password,
                                           final String siteName,
                                           final String folderName,
                                           final DocumentType fileType,
                                           final String docName,
                                           final String docContent) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(docName)
                || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }
        Document d = null;
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        properties.put(PropertyIds.NAME, docName);     
        Session session = getCMISSession(userName, password);
        try
        {
            byte[] content = docContent.getBytes();
            InputStream stream = new ByteArrayInputStream(content);
            ContentStream contentStream = session.getObjectFactory().createContentStream(docName, Long.valueOf(content.length), fileType.type, stream);
            String folderId = getNodeRef(userName, password, siteName, folderName);
            CmisObject folderObj = session.getObject(folderId);
            if(folderObj instanceof Folder)
            {
                Folder f = (Folder)folderObj;           
                d = f.createDocument(properties, contentStream, VersioningState.MAJOR);
                return d;
            }           
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Invalid Site " + siteName, nf);
        }
        catch(CmisContentAlreadyExistsException ae)
        {
            throw new CmisRuntimeException("Document already exits " + siteName, ae);
        }
        catch(CmisInvalidArgumentException ia)
        {
            throw new CmisRuntimeException("Invalid folder " + folderName, ia);
        }
        return d;
    }
    
    /**
     * Delete a document using CMIS
     * 
     * @param userName
     * @param password
     * @param folderName
     * @return true if folder is deleted
     * @throws Exception if error
     */
    public void deleteDocument(final String userName,
                               final String password,
                               final String siteName,
                               final String docName) throws Exception
    {
        String docId;     
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(docName) 
                || StringUtils.isEmpty(siteName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        try
        {
            Session session = getCMISSession(userName, password); 
            docId = getNodeRef(userName, password, siteName, docName);
            session.getObject(docId).delete();
        }
        catch(CmisObjectNotFoundException nf)
        {
            throw new CmisRuntimeException("Invalid site " + siteName, nf);
        }       
    }
    
    /**
     * Delete a parent folder that has children using CMIS
     * 
     * @param userName
     * @param password
     * @param folderName
     * @return true if folder is deleted
     * @throws Exception if error
     */
    public void deleteTree(final String userName,
                           final String password,
                           final String siteName,
                           final String folderName) throws Exception
    {
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(folderName) 
                || StringUtils.isEmpty(folderName))
        {
            throw new IllegalArgumentException("Parameter missing");
        }        
        try
        {
            Session session = getCMISSession(userName, password); 
            String folderId = getNodeRef(userName, password, siteName, folderName);
            CmisObject o = session.getObject(folderId);
            if(o instanceof Folder)
            {
                Folder f = (Folder)o;
                f.deleteTree(true, UnfileObject.DELETE, true);    
            }
            else
            {
                throw new IllegalArgumentException("Object does not exist or is not a folder");
            } 
        }
        catch(CmisInvalidArgumentException nf)
        {
            throw new CmisRuntimeException("Invalid folder " + folderName, nf);
        } 
    }
}
