/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.adaptive.media.journal.web.internal.exportimport;

import com.liferay.adaptive.media.image.html.AMImageHTMLTagFactory;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.exportimport.content.processor.ExportImportContentProcessor;
import com.liferay.exportimport.kernel.lar.ExportImportPathUtil;
import com.liferay.exportimport.kernel.lar.PortletDataContext;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.StagedModel;
import com.liferay.portal.kernel.repository.model.FileEntry;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Alejandro Tardín
 */
@Component(
	property = {
		"model.class.name=com.liferay.journal.model.JournalArticle",
		"service.ranking:Integer=100"
	}
)
public class AMJournalArticleExportImportContentProcessor
	implements ExportImportContentProcessor<String> {

	@Override
	public String replaceExportContentReferences(
			PortletDataContext portletDataContext, StagedModel stagedModel,
			String content, boolean exportReferencedContent,
			boolean escapeContent)
		throws Exception {

		String replacedContent =
			_exportImportContentProcessor.replaceExportContentReferences(
				portletDataContext, stagedModel, content,
				exportReferencedContent, escapeContent);

		AMReferenceExporter amReferenceExporter = new AMReferenceExporter(
			portletDataContext, stagedModel, exportReferencedContent);

		return _amJournalArticleContentHTMLReplacer.replace(
			replacedContent, html -> _replace(html, amReferenceExporter));
	}

	@Override
	public String replaceImportContentReferences(
			PortletDataContext portletDataContext, StagedModel stagedModel,
			String content)
		throws Exception {

		String replacedContent =
			_exportImportContentProcessor.replaceImportContentReferences(
				portletDataContext, stagedModel, content);

		AMEmbeddedReferenceSet amEmbeddedReferenceSet =
			_amEmbeddedReferenceSetFactory.create(
				portletDataContext, stagedModel);

		return _amJournalArticleContentHTMLReplacer.replace(
			replacedContent, html -> _replace(html, amEmbeddedReferenceSet));
	}

	@Reference(unbind = "-")
	public void setAMEmbeddedReferenceSetFactory(
		AMEmbeddedReferenceSetFactory amEmbeddedReferenceSetFactory) {

		_amEmbeddedReferenceSetFactory = amEmbeddedReferenceSetFactory;
	}

	@Reference(unbind = "-")
	public void setAMImageHTMLTagFactory(
		AMImageHTMLTagFactory amImageHTMLTagFactory) {

		_amImageHTMLTagFactory = amImageHTMLTagFactory;
	}

	@Reference(unbind = "-")
	public void setDLAppLocalService(DLAppLocalService dlAppLocalService) {
		_dlAppLocalService = dlAppLocalService;
	}

	@Reference(
		target = "(objectClass=com.liferay.journal.internal.exportimport.content.processor.JournalArticleExportImportContentProcessor)",
		unbind = "-"
	)
	public void setExportImportContentProcessor(
		ExportImportContentProcessor<String> exportImportContentProcessor) {

		_exportImportContentProcessor = exportImportContentProcessor;
	}

	@Override
	public void validateContentReferences(long groupId, String content)
		throws PortalException {

		_exportImportContentProcessor.validateContentReferences(
			groupId, content);

		_amJournalArticleContentHTMLReplacer.replace(
			content,
			html -> {
				Document document = _parseDocument(html);

				for (Element element : document.select("[data-fileEntryId]")) {
					long fileEntryId = Long.valueOf(
						element.attr("data-fileEntryId"));

					_dlAppLocalService.getFileEntry(fileEntryId);
				}

				return html;
			});
	}

	private FileEntry _getFileEntry(long fileEntryId) {
		try {
			return _dlAppLocalService.getFileEntry(fileEntryId);
		}
		catch (PortalException pe) {
			if (_log.isWarnEnabled()) {
				_log.warn(pe, pe);
			}

			return null;
		}
	}

	private Document _parseDocument(String html) {
		Document.OutputSettings outputSettings = new Document.OutputSettings();

		outputSettings.prettyPrint(false);
		outputSettings.syntax(Document.OutputSettings.Syntax.xml);

		Document document = Jsoup.parseBodyFragment(html);

		document.outputSettings(outputSettings);

		return document;
	}

	private Element _parseNode(String tag) {
		Document document = _parseDocument(tag);

		Element bodyElement = document.body();

		return bodyElement.child(0);
	}

	private String _replace(
			String content, AMEmbeddedReferenceSet amEmbeddedReferenceSet)
		throws PortalException {

		Document document = _parseDocument(content);

		Elements elements = document.getElementsByAttribute(
			_ATTRIBUTE_NAME_EXPORT_IMPORT_PATH);

		for (Element element : elements) {
			String path = element.attr(_ATTRIBUTE_NAME_EXPORT_IMPORT_PATH);

			if (!amEmbeddedReferenceSet.containsReference(path)) {
				continue;
			}

			long fileEntryId = amEmbeddedReferenceSet.importReference(path);

			FileEntry fileEntry = _getFileEntry(fileEntryId);

			if (fileEntry == null) {
				continue;
			}

			element.attr("data-fileEntryId", String.valueOf(fileEntryId));
			element.removeAttr(_ATTRIBUTE_NAME_EXPORT_IMPORT_PATH);

			if ("picture".equals(element.tagName())) {
				Elements imgElements = element.getElementsByTag("img");

				Element imgElement = imgElements.first();

				Element picture = _parseNode(
					_amImageHTMLTagFactory.create(
						imgElement.toString(), fileEntry));

				element.html(picture.html());
			}
		}

		Element bodyElement = document.body();

		return bodyElement.html();
	}

	private String _replace(
			String content, AMReferenceExporter amReferenceExporter)
		throws PortalException {

		Document document = _parseDocument(content);

		for (Element element : document.select("[data-fileEntryId]")) {
			long fileEntryId = Long.valueOf(element.attr("data-fileEntryId"));

			FileEntry fileEntry = _dlAppLocalService.getFileEntry(fileEntryId);

			amReferenceExporter.exportReference(fileEntry);

			element.removeAttr("data-fileEntryId");
			element.attr(
				_ATTRIBUTE_NAME_EXPORT_IMPORT_PATH,
				ExportImportPathUtil.getModelPath(fileEntry));
		}

		Element bodyElement = document.body();

		return bodyElement.html();
	}

	private static final String _ATTRIBUTE_NAME_EXPORT_IMPORT_PATH =
		"export-import-path";

	private static final Log _log = LogFactoryUtil.getLog(
		AMJournalArticleExportImportContentProcessor.class);

	private AMEmbeddedReferenceSetFactory _amEmbeddedReferenceSetFactory;
	private AMImageHTMLTagFactory _amImageHTMLTagFactory;
	private final AMJournalArticleContentHTMLReplacer
		_amJournalArticleContentHTMLReplacer =
			new AMJournalArticleContentHTMLReplacer();
	private DLAppLocalService _dlAppLocalService;
	private ExportImportContentProcessor<String> _exportImportContentProcessor;

}