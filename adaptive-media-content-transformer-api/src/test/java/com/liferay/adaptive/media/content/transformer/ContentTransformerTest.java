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

package com.liferay.adaptive.media.content.transformer;

import com.liferay.adaptive.media.exception.AMException;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;

import org.osgi.framework.BundleException;

/**
 * @author Alejandro Tardín
 */
public class ContentTransformerTest {

	@Before
	public void setUp() throws BundleException {
		_contentTransformerHandler.setServiceTrackerMap(_serviceTrackerMap);
	}

	@After
	public final void tearDown() throws Exception {
		_contentTransformerHandler.deactivate();
	}

	@Test
	public void testIgnoresTheContentTransformersForDifferentContentTypes()
		throws Exception {

		ContentTransformerContentType<String> contentTransformerContentTypeA =
			new TestContentTransformerContentType<>();
		ContentTransformerContentType<String> contentTransformerContentTypeB =
			new TestContentTransformerContentType<>();

		String transformedContentA = "transformedContentA";
		String transformedContentB = "transformedContentB";

		_registerContentTransformer(
			contentTransformerContentTypeA, _ORIGINAL_CONTENT,
			transformedContentA);

		_registerContentTransformer(
			contentTransformerContentTypeB, _ORIGINAL_CONTENT,
			transformedContentB);

		Assert.assertEquals(
			transformedContentA,
			_contentTransformerHandler.transform(
				contentTransformerContentTypeA, _ORIGINAL_CONTENT));

		Assert.assertEquals(
			transformedContentB,
			_contentTransformerHandler.transform(
				contentTransformerContentTypeB, _ORIGINAL_CONTENT));
	}

	@Test
	public void testReturnsTheContentTransformedByAChainOfContentTransformers()
		throws Exception {

		String intermediateTransformedContent =
			"intermediateTransformedContent";
		String finalTransformedContent = "finalTransformedContent";

		_registerContentTransformer(
			_contentTransfomerContentType, _ORIGINAL_CONTENT,
			intermediateTransformedContent);

		_registerContentTransformer(
			_contentTransfomerContentType, intermediateTransformedContent,
			finalTransformedContent);

		Assert.assertEquals(
			finalTransformedContent,
			_contentTransformerHandler.transform(
				_contentTransfomerContentType, _ORIGINAL_CONTENT));
	}

	@Test
	public void testReturnsTheContentTransformedByATransformerForAContentType()
		throws Exception {

		String transformedContent = "transformedContent";

		_registerContentTransformer(
			_contentTransfomerContentType, _ORIGINAL_CONTENT,
			transformedContent);

		Assert.assertEquals(
			transformedContent,
			_contentTransformerHandler.transform(
				_contentTransfomerContentType, _ORIGINAL_CONTENT));
	}

	@Test
	public void testReturnsTheSameContentIfATransformerThrowsAnException()
		throws Exception {

		_registerFailingContentTransformer(
			_contentTransfomerContentType, _ORIGINAL_CONTENT);

		Assert.assertSame(
			_ORIGINAL_CONTENT,
			_contentTransformerHandler.transform(
				_contentTransfomerContentType, _ORIGINAL_CONTENT));
	}

	@Test
	public void testReturnsTheSameContentIfThereAreNoContentTransformers() {
		Assert.assertSame(
			_ORIGINAL_CONTENT,
			_contentTransformerHandler.transform(
				_contentTransfomerContentType, _ORIGINAL_CONTENT));
	}

	@Test
	public void testRunsTheOtherTransformersEvenIfOneOfThemFails()
		throws Exception {

		String transformedContent = "transformedContent";

		_registerFailingContentTransformer(
			_contentTransfomerContentType, _ORIGINAL_CONTENT);

		_registerContentTransformer(
			_contentTransfomerContentType, _ORIGINAL_CONTENT,
			transformedContent);

		Assert.assertEquals(
			transformedContent,
			_contentTransformerHandler.transform(
				_contentTransfomerContentType, _ORIGINAL_CONTENT));
	}

	private ContentTransformer<String> _registerContentTransformer(
			ContentTransformerContentType<String> contentTransformerContentType,
			String originalContent, String transformedContent)
		throws Exception {

		ContentTransformer<String> contentTransformer = Mockito.mock(
			ContentTransformer.class);

		Mockito.when(
			contentTransformer.getContentTransformerContentType()
		).thenReturn(
			contentTransformerContentType
		);

		Mockito.when(
			contentTransformer.transform(originalContent)
		).thenReturn(
			transformedContent
		);

		_serviceTrackerMap.register(contentTransformer);

		return contentTransformer;
	}

	private void _registerFailingContentTransformer(
			ContentTransformerContentType<String> contentTransformerContentType,
			String originalContent)
		throws Exception {

		ContentTransformer<String> failingContentTransformer =
			_registerContentTransformer(
				contentTransformerContentType, originalContent, "");

		Mockito.when(
			failingContentTransformer.transform(originalContent)
		).thenThrow(
			new AMException("Do not worry :), this is an expected exception")
		);
	}

	private static final String _ORIGINAL_CONTENT = "originalContent";

	private final ContentTransformerContentType<String>
		_contentTransfomerContentType =
			new TestContentTransformerContentType<>();
	private final ContentTransformerHandler _contentTransformerHandler =
		new ContentTransformerHandler();
	private final MockServiceTrackerMap _serviceTrackerMap =
		new MockServiceTrackerMap();

	private static class TestContentTransformerContentType<T>
		implements ContentTransformerContentType<T> {

		@Override
		public String getKey() {
			return "test";
		}

	}

	private final class MockServiceTrackerMap implements
		ServiceTrackerMap<ContentTransformerContentType,
			List<ContentTransformer>> {

		@Override
		public void close() {
			_contentTransformerMap.clear();
		}

		@Override
		public boolean containsKey(
			ContentTransformerContentType contentTransformerContentType) {

			return _contentTransformerMap.containsKey(
				contentTransformerContentType);
		}

		@Override
		public List<ContentTransformer> getService(
			ContentTransformerContentType contentTransformerContentType) {

			return _contentTransformerMap.get(contentTransformerContentType);
		}

		@Override
		public Set<ContentTransformerContentType> keySet() {
			return _contentTransformerMap.keySet();
		}

		@Override
		public void open() {
		}

		public void register(ContentTransformer contentTransformer) {
			List<ContentTransformer> formNavigatorEntryConfigurationParsers =
				_contentTransformerMap.computeIfAbsent(
					contentTransformer.getContentTransformerContentType(),
					key -> new ArrayList<>());

			formNavigatorEntryConfigurationParsers.add(contentTransformer);
		}

		@Override
		public Collection<List<ContentTransformer>> values() {
			return _contentTransformerMap.values();
		}

		private final Map<ContentTransformerContentType,
			List<ContentTransformer>> _contentTransformerMap = new HashMap<>();

	}

}