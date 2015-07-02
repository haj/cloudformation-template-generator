package com.monsanto.arch.cloudformation.model

import com.monsanto.arch.cloudformation.model.resource.Resource
import com.monsanto.arch.cloudformation.model.simple.SecurityGroupRoutable
import spray.json._
import DefaultJsonProtocol._

import scala.language.implicitConversions

/**
 * Created by Ryan Richt on 2/15/15
 */
case class Template(
                    Description: String,
                    Parameters:  Option[Seq[Parameter]],
                    Conditions:  Option[Seq[Condition]],
                    Mappings:    Option[Seq[Mapping[_]]],
                    Resources:   Option[Seq[Resource[_]]],
                    Outputs:     Option[Seq[Output[_]]],
                    AWSTemplateFormatVersion: String = "2010-09-09"
                   ){

  private def mergeOptionSeq[T](s1: Option[Seq[T]], s2: Option[Seq[T]]): Option[Seq[T]] =
    if(s1.isEmpty && s2.isEmpty) None
    else Some(s1.getOrElse(Seq.empty[T]) ++ s2.getOrElse(Seq.empty[T]))

  def ++(t: Template) = Template(
    Description + t.Description,
    mergeOptionSeq(Parameters, t.Parameters ),
    mergeOptionSeq(Conditions, t.Conditions ),
    mergeOptionSeq(Mappings,   t.Mappings   ),
    mergeOptionSeq(Resources,  t.Resources  ),
    mergeOptionSeq(Outputs,    t.Outputs    ),
    this.AWSTemplateFormatVersion
  )
}
object Template extends DefaultJsonProtocol {

  val EMPTY = Template("", None, None, None, None, None)

  def collapse[R <: Resource[R]](rs: Seq[R]) = rs.foldLeft(Template.EMPTY)(_ ++ _)

  // b/c we really dont need to implement READING yet, and its a bit trickier
  implicit def optionWriter[T : JsonWriter]: JsonWriter[Option[T]] = new JsonWriter[Option[T]] {
    def write(option: Option[T]) = option match {
      case Some(x) => x.toJson
      case None => JsNull
    }
  }

  implicit val format: JsonWriter[Template] = new JsonWriter[Template]{
    def write(p: Template) = {
      val fields = new collection.mutable.ListBuffer[(String, JsValue)]
      fields ++= productElement2Field[String]("AWSTemplateFormatVersion", p, 6)
      fields ++= productElement2Field[String]("Description", p, 0)
      if(p.Parameters.nonEmpty) fields ++= productElement2Field[Option[Seq[Parameter]]]("Parameters", p, 1)
      if(p.Conditions.nonEmpty) fields ++= productElement2Field[Option[Seq[Condition]]]("Conditions", p, 2)
      if(p.Mappings.nonEmpty) fields ++= productElement2Field[Option[Seq[Mapping[_]]]]("Mappings", p, 3)
      if(p.Resources.nonEmpty) fields ++= productElement2Field[Option[Seq[Resource[_]]]]("Resources", p, 4)
      if(p.Outputs.nonEmpty) fields ++= productElement2Field[Option[Seq[Output[_]]]]("Outputs", p, 5)
      JsObject(fields: _*)
    }
  }

  implicit def fromResource[R <: Resource[R]](r: R): Template = Template("", None, None, None, Some(Seq(r)), None)
  implicit def fromOutput(o: Output[_]): Template = Template("", None, None, None, None, Some(Seq(o)))
  implicit def fromSecurityGroupRoutable[R <: Resource[R]](sgr: SecurityGroupRoutable[R]): Template = sgr.template
}