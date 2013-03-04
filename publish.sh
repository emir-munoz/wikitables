#!/bin/sh

# Push files @Emir Muñoz
BRANCH="$(git rev-parse --symbolic --abbrev-ref $(git symbolic-ref HEAD))"

ADD="$(git add -A .)"
test -x "$ADD" && exec "$ADD" "$@"

COMMIT="$(git commit -m 'Updates wikitable')"
test -x "$COMMIT" && exec "$COMMIT" "$@"

PUSH="$(git push origin $BRANCH)"
test -x "PUSH" && exec "$PUSH" "$@"
