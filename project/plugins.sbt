resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables" % "2.2.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"     % "3.9.0")
addSbtPlugin("com.typesafe.play" % "sbt-plugin"         % "2.8.19")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"       % "2.4.6")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"      % "1.9.3")
