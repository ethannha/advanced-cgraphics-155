#version 430

uniform float offset;
uniform int motion;
uniform float scale;
uniform int rotate;
out vec4 vertColor;

void main(void)
{ 
  if(motion == 1) {
    //direction point up ^
    if (rotate == 1) {
      if (gl_VertexID == 0) { //bot-right
        gl_Position = vec4((scale*0.1)+cos(offset), (scale*-0.2)+sin(offset), 0.0, 1.0);
        vertColor = vec4(0.0, 0.0, 1.0, 1.0);
      }
      else if (gl_VertexID == 1) {  //bot-left
        gl_Position = vec4((scale*-0.1)+cos(offset), (scale*-0.2)+sin(offset), 0.0, 1.0);
        vertColor = vec4(0.0, 1.0, 0.0, 1.0);
      }
      else {  //top-middle
        gl_Position = vec4((scale*0.0)+cos(offset), (scale*0.2)+sin(offset), 0.0, 1.0);
        vertColor = vec4(1.0, 0.0, 0.0, 1.0);
      }
    }
    //direction point left <
    else if (rotate == 2) {
      if (gl_VertexID == 0) { //bot-right
        gl_Position = vec4((scale*0.2)+cos(offset), (scale*-0.1)+sin(offset), 0.0, 1.0);
        vertColor = vec4(0.0, 0.0, 1.0, 1.0);
      }
      else if (gl_VertexID == 1) {  //mid-left
        gl_Position = vec4((scale*-0.2)+cos(offset), (scale*0.0)+sin(offset), 0.0, 1.0);
        vertColor = vec4(0.0, 1.0, 0.0, 1.0);
      }
      else {  //top-right
        gl_Position = vec4((scale*0.2)+cos(offset), (scale*0.1)+sin(offset), 0.0, 1.0);
        vertColor = vec4(1.0, 0.0, 0.0, 1.0);
      }
    }
    //direction point down V
    else if (rotate == 3) {
      if (gl_VertexID == 0) { //top-right
        gl_Position = vec4((scale*-0.1)+cos(offset), (scale*0.2)+sin(offset), 0.0, 1.0);
        vertColor = vec4(0.0, 0.0, 1.0, 1.0);
      }
      else if (gl_VertexID == 1) {  //top-left
        gl_Position = vec4((scale*0.1)+cos(offset), (scale*0.2)+sin(offset), 0.0, 1.0);
        vertColor = vec4(0.0, 1.0, 0.0, 1.0);
      }
      else {  //bot-middle
        gl_Position = vec4((scale*0.0)+cos(offset), (scale*-0.2)+sin(offset), 0.0, 1.0);
        vertColor = vec4(1.0, 0.0, 0.0, 1.0);
      }
    }
    // direction point right >
    else if (rotate == 4) {
      if (gl_VertexID == 0) { //bot-left
        gl_Position = vec4((scale*-0.2)+cos(offset), (scale*0.1)+sin(offset), 0.0, 1.0);
        vertColor = vec4(0.0, 0.0, 1.0, 1.0);
      }
      else if (gl_VertexID == 1) {  //mid-right
        gl_Position = vec4((scale*0.2)+cos(offset), (scale*0.0)+sin(offset), 0.0, 1.0);
        vertColor = vec4(0.0, 1.0, 0.0, 1.0);
      }
      else {  //top-left
        gl_Position = vec4((scale*-0.2)+cos(offset), (scale*-0.1)+sin(offset), 0.0, 1.0);
        vertColor = vec4(1.0, 0.0, 0.0, 1.0);
      }
    }
  }
  else {
    //direction point up ^
    if (rotate == 1) {
      if (gl_VertexID == 0) { //bot-right
        gl_Position = vec4((scale*0.1), (scale*-0.2), 0.0, 1.0);
        vertColor = vec4(0.0, 0.0, 1.0, 1.0);
      }
      else if (gl_VertexID == 1) {  //bot-left
        gl_Position = vec4((scale*-0.1), (scale*-0.2), 0.0, 1.0);
        vertColor = vec4(0.0, 1.0, 0.0, 1.0);
      }
      else {  //top-middle
        gl_Position = vec4((scale*0.0), (scale*0.2), 0.0, 1.0);
        vertColor = vec4(1.0, 0.0, 0.0, 1.0);
      }
    }
    //direction point left <
    else if (rotate == 2) {
      if (gl_VertexID == 0) { //bot-right
        gl_Position = vec4((scale*0.2), (scale*-0.1), 0.0, 1.0);
        vertColor = vec4(0.0, 0.0, 1.0, 1.0);
      }
      else if (gl_VertexID == 1) {  //mid-left
        gl_Position = vec4((scale*-0.2), (scale*0.0), 0.0, 1.0);
        vertColor = vec4(0.0, 1.0, 0.0, 1.0);
      }
      else {  //top-right
        gl_Position = vec4((scale*0.2), (scale*0.1), 0.0, 1.0);
        vertColor = vec4(1.0, 0.0, 0.0, 1.0);
      }
    }
    //direction point down V
    else if (rotate == 3) {
      if (gl_VertexID == 0) { //top-right
        gl_Position = vec4((scale*-0.1), (scale*0.2), 0.0, 1.0);
        vertColor = vec4(0.0, 0.0, 1.0, 1.0);
      }
      else if (gl_VertexID == 1) {  //top-left
        gl_Position = vec4((scale*0.1), (scale*0.2), 0.0, 1.0);
        vertColor = vec4(0.0, 1.0, 0.0, 1.0);
      }
      else {  //bot-middle
        gl_Position = vec4((scale*0.0), (scale*-0.2), 0.0, 1.0);
        vertColor = vec4(1.0, 0.0, 0.0, 1.0);
      }
    }
    // direction point right >
    else if (rotate == 4) {
      if (gl_VertexID == 0) { //bot-left
        gl_Position = vec4((scale*-0.2), (scale*0.1), 0.0, 1.0);
        vertColor = vec4(0.0, 0.0, 1.0, 1.0);
      }
      else if (gl_VertexID == 1) {  //mid-right
        gl_Position = vec4((scale*0.2), (scale*0.0), 0.0, 1.0);
        vertColor = vec4(0.0, 1.0, 0.0, 1.0);
      }
      else {  //top-left
        gl_Position = vec4((scale*-0.2), (scale*-0.1), 0.0, 1.0);
        vertColor = vec4(1.0, 0.0, 0.0, 1.0);
      }
    }
  }
}
