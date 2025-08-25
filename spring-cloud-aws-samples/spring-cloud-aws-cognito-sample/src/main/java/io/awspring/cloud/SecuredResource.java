/*
 * Copyright 2013-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.awspring.cloud;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo secured resource.
 *
 * @author Oleh Onufryk
 * @since 3.3.0
 */

@RestController
@RequestMapping("/api/secured")
public class SecuredResource {

	@GetMapping
	@PreAuthorize("@securityDecisionMaker.hasPermission(authentication, 'READ')")
	public String secured() {
		return """
				{
				    "message": "This is a secured endpoint"
				}
				""";
	}
}
