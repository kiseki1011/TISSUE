-- A comment can carry a review verdict: the reviewer's feedback body is stored as an ordinary comment
-- so it joins the issue's conversation (replies, mentions, edit, soft-delete all come for free), with
-- this column recording which verdict it was submitted with. NULL means an ordinary comment.
--
-- The verdict is frozen on the row on purpose. issue_reviewer holds only the *current* status and
-- resetReview() overwrites it, so a past APPROVED would otherwise be unrecoverable once the author
-- re-requests a review. Denormalizing it here makes each comment self-describing.
ALTER TABLE public.comment ADD COLUMN review_status character varying(255);

ALTER TABLE public.comment
    ADD CONSTRAINT comment_review_status_check
    CHECK (review_status IS NULL OR (review_status)::text = ANY ((ARRAY[
        'PENDING', 'APPROVED', 'CHANGES_REQUESTED']::character varying[])::text[]));
