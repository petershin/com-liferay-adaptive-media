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

package com.liferay.adaptive.media.image.content.transformer.backwards.compatibility.internal;

import com.liferay.adaptive.media.content.transformer.ContentTransformer;
import com.liferay.adaptive.media.content.transformer.ContentTransformerContentType;
import com.liferay.adaptive.media.content.transformer.constants.ContentTransformerContentTypes;
import com.liferay.adaptive.media.image.html.AMImageHTMLTagFactory;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.repository.model.FileEntry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Adolfo Pérez
 */
@Component(
	immediate = true, property = "content.transformer.content.type=html",
	service = ContentTransformer.class
)
public class AMBackwardsCompatibilityHtmlContentTransformer
	implements ContentTransformer<String> {

	@Override
	public ContentTransformerContentType<String>
		getContentTransformerContentType() {

		return ContentTransformerContentTypes.HTML;
	}

	@Override
	public String transform(String html) throws PortalException {
		if (html == null) {
			return null;
		}

		Matcher matcher = _dlImageUrlPattern.matcher(html);

		StringBuffer sb = new StringBuffer(html.length());

		while (matcher.find()) {
			FileEntry fileEntry = _getFileEntry(matcher);

			String imgTag = matcher.group(0);

			String adaptiveTag = _amImageHTMLTagFactory.create(
				imgTag, fileEntry);

			matcher.appendReplacement(
				sb, Matcher.quoteReplacement(adaptiveTag));
		}

		matcher.appendTail(sb);

		return sb.toString();
	}

	@Reference(unbind = "-")
	protected void setAMImageHTMLTagFactory(
		AMImageHTMLTagFactory amImageHTMLTagFactory) {

		_amImageHTMLTagFactory = amImageHTMLTagFactory;
	}

	@Reference(unbind = "-")
	protected void setDLAppLocalService(DLAppLocalService dlAppLocalService) {
		_dlAppLocalService = dlAppLocalService;
	}

	private FileEntry _getFileEntry(Matcher matcher) throws PortalException {
		if (matcher.group(4) != null) {
			long groupId = Long.valueOf(matcher.group(1));
			String uuid = matcher.group(4);

			return _dlAppLocalService.getFileEntryByUuidAndGroupId(
				uuid, groupId);
		}

		long groupId = Long.valueOf(matcher.group(1));
		long folderId = Long.valueOf(matcher.group(2));
		String title = matcher.group(3);

		return _dlAppLocalService.getFileEntry(groupId, folderId, title);
	}

	private static final Pattern _dlImageUrlPattern = Pattern.compile(
		"<img\\s+src=['\"]/documents/(\\d+)/(\\d+)/([^/?]+)" +
			"(?:/([-0-9a-fA-F]+))?(?:\\?t=\\d+)?['\"]\\s*/>");

	private AMImageHTMLTagFactory _amImageHTMLTagFactory;
	private DLAppLocalService _dlAppLocalService;

}