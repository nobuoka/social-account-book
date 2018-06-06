FROM alpine

RUN apk add --no-cache curl
COPY ./test.sh /test.sh

ENTRYPOINT ["sh"]
CMD ["/test.sh"]
