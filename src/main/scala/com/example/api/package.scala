package com.example

import zio.Has

package object api {
  type Api = Has[Api.Service]
  type GraphQLApi = Has[graphql.GraphQLApi.Service]
}
