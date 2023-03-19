package Remoa.BE.Post.Controller;

import Remoa.BE.Member.Domain.Comment;
import Remoa.BE.Member.Domain.Member;
import Remoa.BE.Member.Dto.Res.ResMemberInfoDto;
import Remoa.BE.Member.Service.MemberService;
import Remoa.BE.Post.Domain.Post;
import Remoa.BE.Post.Dto.Response.ResCommentDto;
import Remoa.BE.Post.Dto.Response.ResReceivedCommentDto;
import Remoa.BE.Post.Dto.Response.ResReplyDto;
import Remoa.BE.Post.Service.CommentService;
import Remoa.BE.Post.Service.MyPostService;
import Remoa.BE.exception.CustomMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static Remoa.BE.exception.CustomBody.errorResponse;
import static Remoa.BE.exception.CustomBody.successResponse;
import static Remoa.BE.utill.MemberInfo.authorized;
import static Remoa.BE.utill.MemberInfo.getMemberId;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MyFeedbackController {

    private final MemberService memberService;
    private final MyPostService myPostService;
    private final CommentService commentService;

    @GetMapping("/user/feedback")
    public ResponseEntity<Object> receivedFeedback(HttpServletRequest request,
                                                   @RequestParam(required = false, defaultValue = "all") String category,
                                                   @RequestParam(required = false, defaultValue = "1", name = "page") int pageNumber) {

        if (authorized(request)) {
            Long memberId = getMemberId();
            Member myMember = memberService.findOne(memberId);

            pageNumber -= 1;
            if (pageNumber < 0) {
                return errorResponse(CustomMessage.PAGE_NUM_OVER);
            }

            Page<Post> posts;
            if (category.equals("idea") ||
                    category.equals("marketing") ||
                    category.equals("design") ||
                    category.equals("video") ||
                    category.equals("etc")) {

                posts = myPostService.getNewestThreePostsSortCategory(pageNumber, myMember, category);

            } else {
                posts = myPostService.getNewestThreePosts(pageNumber, myMember);
            }

            Map<String, ResReceivedCommentDto> result = new HashMap<>();

            int postNumber = 1;
            for (Post post : posts) { //조회한 post
                Map<String, ResCommentDto> commentInfo = new HashMap<>();

                List<Comment> parentComments = commentService.getRecentThreeCommentsExceptReply(post);

                int commentNumber = 1;
                for (Comment parentComment : parentComments) { //조회한 post의 parent comment
                    List<Comment> parentCommentsReply = commentService.getParentCommentsReply(parentComment);



                    List<ResReplyDto> replies = new ArrayList<>();
                    int replyNum = 1;
                    for (Comment reply : parentCommentsReply) {
                        log.warn("reply = {}", reply.getComment());

                        replies.add(new ResReplyDto(
                                reply.getMember().getMemberId(),
                                reply.getCommentId(),
                                reply.getMember().getNickname(),
                                reply.getMember().getProfileImage(),
                                reply.getComment(),
                                reply.getCommentLikeCount()));
                        replyNum++;
                    }

                    commentInfo.put("comment_" + commentNumber, new ResCommentDto(
                            parentComment.getMember().getMemberId(),
                            parentComment.getCommentId(),
                            parentComment.getMember().getNickname(),
                            parentComment.getMember().getProfileImage(),
                            parentComment.getComment(),
                            parentComment.getCommentLikeCount(),
                            replies));

                    commentNumber++;
                }

                ResReceivedCommentDto map = ResReceivedCommentDto.builder()
                        .title(post.getTitle())
                        .postId(post.getPostId())
                        .postMember(new ResMemberInfoDto(post.getMember().getMemberId(),
                                post.getMember().getNickname(),
                                post.getMember().getProfileImage()))
                        .commentInfo(commentInfo)
                        .build();

                result.put("post_" + postNumber, map);

                postNumber++;
            }

            return successResponse(CustomMessage.OK, result);

        }
        return errorResponse(CustomMessage.UNAUTHORIZED);
    }

}
