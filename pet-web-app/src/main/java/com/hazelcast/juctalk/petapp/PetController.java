package com.hazelcast.juctalk.petapp;

import com.hazelcast.juctalk.Photo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping("/photo/current")
    public Photo getCurrentPhoto() {
        return petService.getCurrentPhoto();
    }

    @GetMapping(value = "/photo/{name}", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] getPhoto(@PathVariable String name) throws IOException {
        return petService.getPhoto(name);
    }
}
