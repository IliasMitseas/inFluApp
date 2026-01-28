package org.ilias.influapp.services;

import org.ilias.influapp.dtos.PostDto;
import org.ilias.influapp.entities.Post;
import org.ilias.influapp.entities.SocialMedia;

public interface PostService {
    Post createPostFromDto(PostDto postDto, SocialMedia socialMedia);
}
