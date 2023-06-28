/*
 *
 * Copyright 2017 Uncharted Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.uncharted.mugshots.rest;

import com.uncharted.mugshots.service.ImageDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MugshotController {

    private final ImageDataService imageDataService;

    @PostMapping(value = "/index-images")
    public ResponseEntity<String> indexImages(@RequestParam("image") MultipartFile[] files) throws Exception {
        imageDataService.storeImages(files);
        return ResponseEntity.ok("yay");
    }

    @GetMapping(value = "/find-images")
    public ResponseEntity<List<byte[]>> findImages(@RequestParam("image") MultipartFile file) throws IOException {
        var results = imageDataService.findByImage(file);
        return ResponseEntity.ok(results);
    }

    @PostMapping(value = "/edit-image")
    public ResponseEntity<String> editImage() {
        return ResponseEntity.ok("yay");
    }

}
