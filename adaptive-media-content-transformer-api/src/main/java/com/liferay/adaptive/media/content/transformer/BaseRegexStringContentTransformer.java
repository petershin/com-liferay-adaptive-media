/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.adaptive.media.content.transformer;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.util.Validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Adolfo Pérez
 */
public abstract class BaseRegexStringContentTransformer
	implements ContentTransformer<String> {

	@Override
	public abstract ContentTransformerContentType<String>
		getContentTransformerContentType();

	@Override
	public String transform(String content) throws PortalException {
		if (Validator.isNull(content)) {
			return content;
		}

		Pattern pattern = getPattern();

		Matcher matcher = pattern.matcher(content);

		StringBuffer sb = new StringBuffer(content.length());

		while (matcher.find()) {
			FileEntry fileEntry = getFileEntry(matcher);

			String imgTag = matcher.group(0);

			String amImageTag = createAMImageTag(imgTag, fileEntry);

			matcher.appendReplacement(sb, Matcher.quoteReplacement(amImageTag));
		}

		matcher.appendTail(sb);

		return sb.toString();
	}

	protected abstract String createAMImageTag(
			String originalImgTag, FileEntry fileEntry)
		throws PortalException;

	protected abstract FileEntry getFileEntry(Matcher matcher)
		throws PortalException;

	protected abstract Pattern getPattern();

}