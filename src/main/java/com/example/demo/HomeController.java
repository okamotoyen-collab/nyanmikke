package com.example.demo;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Controller
public class HomeController {

    private final PostRepository postRepository;
    private final PhotoRepository photoRepository;

    public HomeController(
            PostRepository postRepository,
            PhotoRepository photoRepository) {

        this.postRepository = postRepository;
        this.photoRepository = photoRepository;
    }

    @GetMapping("/")
    public String index(
            @RequestParam(required = false) String tag,
            Model model) {

        List<Post> posts;

        if (tag == null || tag.equals("すべて")) {
            posts = postRepository.findAllByOrderByIdDesc();
        } else {
            posts = postRepository.findByTagOrderByIdDesc(tag);
        }

        model.addAttribute("posts", posts);
        model.addAttribute("photoRepository", photoRepository);
        model.addAttribute("selectedTag", tag);

        return "index";
    }

    @PostMapping("/add")
    public String addPost(

            @RequestParam("photos") MultipartFile[] photos,
            @RequestParam String place,
            @RequestParam String tag,
            @RequestParam String memo) throws IOException {

        Post post = new Post();

        post.setPlace(place);
        post.setMemo(memo);
        post.setTag(tag);
        post.setLikes(0);

        String now = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));

        post.setCreatedAt(now);

        postRepository.save(post);

        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dtuk0eysm",
                "api_key", "676356985268981",
                "api_secret", "g_rSkXtgLwAmvHXpR-Yp8zVEb20"
        ));

        for (MultipartFile photo : photos) {

            if (!photo.isEmpty()) {

                Map uploadResult = cloudinary.uploader().upload(
                        photo.getBytes(),
                        ObjectUtils.asMap("folder", "nyanmikke")
                );

                String imageUrl =
                        uploadResult.get("secure_url").toString();

                Photo p = new Photo();

                p.setImageUrl(imageUrl);
                p.setPostId(post.getId());

                photoRepository.save(p);
            }
        }

        return "redirect:/";
    }
    @PostMapping("/delete")
    public String deletePost(@RequestParam Long id) {

        List<Photo> photos =
                photoRepository.findByPostId(id);

        photoRepository.deleteAll(photos);

        postRepository.deleteById(id);

        return "redirect:/";
    }
    @PostMapping("/like")
    public String likePost(@RequestParam Long id) {

        Post post = postRepository.findById(id).orElse(null);

        if (post != null) {
            post.setLikes(post.getLikes() + 1);
            postRepository.save(post);
        }

        return "redirect:/";
    }

    @GetMapping("/detail")
    public String detail(@RequestParam Long id, Model model) {

        Post post = postRepository.findById(id).orElse(null);

        model.addAttribute("post", post);
        model.addAttribute("photos", photoRepository.findByPostId(id));

        return "detail";
    }
}
