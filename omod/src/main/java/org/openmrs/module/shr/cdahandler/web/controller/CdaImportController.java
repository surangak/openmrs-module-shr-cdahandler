/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.shr.cdahandler.web.controller;

import java.io.InputStream;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openhealthtools.mdht.uml.cda.ClinicalDocument;
import org.openhealthtools.mdht.uml.cda.util.CDAUtil;
import org.openmrs.Encounter;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaDocumentModel;
import org.openmrs.module.shr.cdahandler.api.DocumentParseException;
import org.openmrs.module.shr.cdahandler.api.cdaAntepartumService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

/**
 * The main controller.
 */
@Controller
@SessionAttributes("document")
public class CdaImportController {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	@RequestMapping(value = "/module/shr-cdahandler/import", method = RequestMethod.GET)
	public void importGET(ModelMap model) {
		if (model.get("document") == null)
			model.put("document", new Document());
	}
	
	@RequestMapping(value = "/module/shr-cdahandler/import", method = RequestMethod.POST)
	public ModelAndView importPOST(HttpServletRequest request, HttpServletResponse response,
	        @ModelAttribute("document") Document document, @RequestParam(value = "importFile") MultipartFile file)
	        throws Throwable {
		if (file == null || file.isEmpty())
			return new ModelAndView("redirect:import.form");
		
		log.info("User uploaded document " + file.getOriginalFilename());
		
		try {
			Encounter e = Context.getService(cdaAntepartumService.class).importAntepartumHistoryAndPhysical(
			    file.getInputStream());
			log.info("Successfully imported document. Generated encounter with id ");
			
			document.transformCDAtoHTML(file.getInputStream());
			
		}
		catch (DocumentParseException e) {
			log.warn("Invalid CDA document uploaded", e);
		}
		catch (Throwable e) {
			log.error("", e);
			throw e;
		}
		return new ModelAndView("redirect:import.form");
	}
	
	public static class Document {
		
		private String html;
		
		private void transformCDAtoHTML(InputStream in) throws TransformerException {
			TransformerFactory factory = TransformerFactory.newInstance();
			Source xslt = new StreamSource(getClass().getClassLoader().getResourceAsStream("cda.xsl"));
			Transformer transformer = factory.newTransformer(xslt);
			
			Source text = new StreamSource(in);
			StringWriter sw = new StringWriter();
			transformer.transform(text, new StreamResult(sw));
			html = sw.toString();
			applyFormatting();
			System.out.println(html);
		}
		
		private void applyFormatting() {
			html = html.substring(html.indexOf("<body>") + "<body>".length());
			html = html.substring(0, html.indexOf("</body>"));
		}
		
		public String getHtml() {
			return html;
		}
	}
}
